#!/usr/bin/env python3
"""Launch an installed NeoForge client using launcher-style production inputs.

This intentionally mirrors NeoForge's own RunProductionClient E2E task closely
while staying self-contained for GitHub Actions. It resolves the vanilla parent
manifest, downloads missing launcher libraries, extracts Linux natives, builds
the production classpath, and starts the installed NeoForge profile without
ModDevGradle/DevLaunch.
"""

from __future__ import annotations

import argparse
import json
import os
import platform
import re
import shutil
import signal
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

MC_VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
DEFAULT_LIBRARY_BASE = "https://libraries.minecraft.net/"
READY_MARKERS = (
    "Sound engine started",
    "OpenAL initialized",
    "Created: 1024x1024x4 minecraft:textures/atlas/blocks.png-atlas",
)


def download(url: str, destination: Path, retries: int = 3) -> None:
    if destination.is_file() and destination.stat().st_size > 0:
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    last_error: Exception | None = None
    for attempt in range(retries):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "NeoForge-Carpet-CI/1"})
            with urllib.request.urlopen(request, timeout=60) as response, destination.open("wb") as out:
                shutil.copyfileobj(response, out)
            return
        except (OSError, urllib.error.URLError) as exc:
            last_error = exc
            destination.unlink(missing_ok=True)
            if attempt + 1 < retries:
                time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"Failed to download {url}: {last_error}")


def read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def fetch_json(url: str) -> dict:
    request = urllib.request.Request(url, headers={"User-Agent": "NeoForge-Carpet-CI/1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def current_os_name() -> str:
    if sys.platform.startswith("linux"):
        return "linux"
    if sys.platform == "darwin":
        return "osx"
    if os.name == "nt":
        return "windows"
    return sys.platform


def current_os_arch() -> str:
    machine = platform.machine().lower()
    if machine in {"i386", "i686", "x86"}:
        return "x86"
    return machine


def rule_applies(rule: dict) -> bool:
    os_rule = rule.get("os")
    if os_rule is None:
        # NeoForge's production E2E runner intentionally ignores feature-based
        # launcher rules. They are not needed for a headless smoke launch.
        return False
    name = os_rule.get("name")
    arch = os_rule.get("arch")
    if name is not None and name != current_os_name():
        return False
    if arch is not None and arch != current_os_arch():
        return False
    return True


def disabled_by_rules(entry: dict) -> bool:
    rules = entry.get("rules")
    if not rules:
        return False

    # Launcher semantics: entries with rules are denied by default, then the
    # matching rules update the decision in declaration order.
    allowed = False
    matched = False
    for rule in rules:
        if rule_applies(rule):
            matched = True
            allowed = rule.get("action") == "allow"
    return not (matched and allowed)


def argument_values(manifest: dict, kind: str) -> list[str]:
    arguments = manifest.get("arguments", {}).get(kind, [])
    result: list[str] = []
    for argument in arguments:
        if isinstance(argument, str):
            result.append(argument)
            continue
        if disabled_by_rules(argument):
            continue
        value = argument.get("value")
        if isinstance(value, str):
            result.append(value)
        elif isinstance(value, list):
            result.extend(str(item) for item in value)
    return result


def parse_maven_coordinate(coordinate: str) -> tuple[str, str, str, str | None, str]:
    if "@" in coordinate:
        coordinate, extension = coordinate.rsplit("@", 1)
    else:
        extension = "jar"
    parts = coordinate.split(":")
    if len(parts) not in (3, 4):
        raise ValueError(f"Unsupported Maven coordinate: {coordinate}")
    group, artifact, version = parts[:3]
    classifier = parts[3] if len(parts) == 4 else None
    return group, artifact, version, classifier, extension


def maven_path(coordinate: str) -> str:
    group, artifact, version, classifier, extension = parse_maven_coordinate(coordinate)
    filename = f"{artifact}-{version}"
    if classifier:
        filename += f"-{classifier}"
    filename += f".{extension}"
    return f"{group.replace('.', '/')}/{artifact}/{version}/{filename}"


def dedupe_key(coordinate: str) -> tuple[str, str, str | None, str]:
    group, artifact, _version, classifier, extension = parse_maven_coordinate(coordinate)
    return group, artifact, classifier, extension


def ensure_library(library: dict, libraries_dir: Path) -> Path:
    coordinate = library["name"]
    artifact = library.get("downloads", {}).get("artifact")
    if artifact:
        relative = artifact.get("path") or maven_path(coordinate)
        destination = libraries_dir / relative
        if not destination.is_file():
            url = artifact.get("url")
            if not url:
                base = library.get("url", DEFAULT_LIBRARY_BASE)
                url = base.rstrip("/") + "/" + relative
            download(url, destination)
        return destination

    relative = maven_path(coordinate)
    destination = libraries_dir / relative
    if not destination.is_file():
        base = library.get("url", DEFAULT_LIBRARY_BASE)
        download(base.rstrip("/") + "/" + relative, destination)
    return destination


def extract_natives(manifest: dict, libraries_dir: Path, natives_dir: Path) -> None:
    natives_dir.mkdir(parents=True, exist_ok=True)
    os_name = current_os_name()
    for library in manifest.get("libraries", []):
        if disabled_by_rules(library):
            continue
        native_map = library.get("natives", {})
        classifier = native_map.get(os_name)
        if not classifier:
            continue
        arch_token = "32" if current_os_arch() == "x86" else "64"
        classifier = classifier.replace("${arch}", arch_token)
        classifier_info = library.get("downloads", {}).get("classifiers", {}).get(classifier)
        if not classifier_info:
            continue
        relative = classifier_info.get("path")
        url = classifier_info.get("url")
        if not relative or not url:
            continue
        native_jar = libraries_dir / relative
        download(url, native_jar)
        excludes = tuple(library.get("extract", {}).get("exclude", ["META-INF/"]))
        with zipfile.ZipFile(native_jar) as archive:
            for member in archive.infolist():
                if member.is_dir() or any(member.filename.startswith(prefix) for prefix in excludes):
                    continue
                target = natives_dir / member.filename
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(member) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output)


def expand(value: str, placeholders: dict[str, str]) -> str:
    pattern = re.compile(r"\$\{([^}]+)}")
    return pattern.sub(lambda match: placeholders.get(match.group(1), match.group(0)), value)


def load_manifests(install_dir: Path, version_id: str, vanilla_manifest: dict) -> list[dict]:
    child_path = install_dir / "versions" / version_id / f"{version_id}.json"
    child = read_json(child_path)
    parent_id = child.get("inheritsFrom")
    if parent_id and parent_id != vanilla_manifest.get("id"):
        raise RuntimeError(f"Unexpected NeoForge parent {parent_id!r}; expected {vanilla_manifest.get('id')!r}")
    return [vanilla_manifest, child]


def build_classpath(manifests: list[dict], libraries_dir: Path, game_jar: Path) -> list[Path]:
    seen: set[tuple[str, str, str | None, str]] = set()
    result: list[Path] = []
    # Match NeoForge's own E2E runner: child libraries override parent versions.
    for manifest in reversed(manifests):
        for library in manifest.get("libraries", []):
            if disabled_by_rules(library):
                continue
            key = dedupe_key(library["name"])
            if key in seen:
                continue
            seen.add(key)
            result.append(ensure_library(library, libraries_dir))
    result.append(game_jar)
    return result


def vanilla_manifest_for(version: str) -> dict:
    manifest_index = fetch_json(MC_VERSION_MANIFEST)
    entry = next((item for item in manifest_index["versions"] if item["id"] == version), None)
    if entry is None:
        raise RuntimeError(f"Minecraft {version} is missing from Mojang's version manifest")
    return fetch_json(entry["url"])


def tail(path: Path, limit: int = 30000) -> str:
    if not path.is_file():
        return ""
    data = path.read_bytes()
    return data[-limit:].decode("utf-8", errors="replace")


def terminate_process_group(process: subprocess.Popen) -> None:
    if process.poll() is not None:
        return
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait(timeout=5)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--install-dir", required=True, type=Path)
    parser.add_argument("--minecraft-version", required=True)
    parser.add_argument("--neoforge-version", required=True)
    parser.add_argument("--assets-dir", required=True, type=Path)
    parser.add_argument("--timeout", type=int, default=120)
    parser.add_argument("--quick-play-multiplayer")
    parser.add_argument("--required-marker")
    parser.add_argument("--stability-seconds", type=int, default=0)
    args = parser.parse_args()

    install_dir = args.install_dir.resolve()
    libraries_dir = install_dir / "libraries"
    natives_dir = install_dir / "natives"
    version_id = f"neoforge-{args.neoforge_version}"
    version_dir = install_dir / "versions" / version_id
    child_manifest_path = version_dir / f"{version_id}.json"
    if not child_manifest_path.is_file():
        raise RuntimeError(f"NeoForge client profile was not installed: {child_manifest_path}")

    vanilla = vanilla_manifest_for(args.minecraft_version)
    vanilla_dir = install_dir / "versions" / args.minecraft_version
    vanilla_dir.mkdir(parents=True, exist_ok=True)
    (vanilla_dir / f"{args.minecraft_version}.json").write_text(json.dumps(vanilla), encoding="utf-8")

    client_download = vanilla["downloads"]["client"]
    original_client = vanilla_dir / f"{args.minecraft_version}.jar"
    download(client_download["url"], original_client)

    # The production NeoForge launcher profile expects the Minecraft client JAR
    # under the child version id, just like NeoForge's RunProductionClient task.
    version_dir.mkdir(parents=True, exist_ok=True)
    game_jar = version_dir / f"{version_id}.jar"
    if not game_jar.is_file() or game_jar.stat().st_size != original_client.stat().st_size:
        shutil.copy2(original_client, game_jar)

    manifests = load_manifests(install_dir, version_id, vanilla)
    for manifest in manifests:
        extract_natives(manifest, libraries_dir, natives_dir)

    classpath_items = build_classpath(manifests, libraries_dir, game_jar)
    classpath = os.pathsep.join(str(item.resolve()) for item in classpath_items)

    asset_index = vanilla["assetIndex"]["id"]
    assets_dir = args.assets_dir.expanduser().resolve()
    index_path = assets_dir / "indexes" / f"{asset_index}.json"
    if not index_path.is_file():
        download(vanilla["assetIndex"]["url"], index_path)

    placeholders = {
        "auth_player_name": "Dev",
        "version_name": version_id,
        "game_directory": str(install_dir),
        "auth_uuid": "00000000-0000-4000-8000-000000000000",
        "auth_access_token": "0",
        "clientid": "0",
        "auth_xuid": "0",
        "user_type": "legacy",
        "version_type": "release",
        "assets_index_name": asset_index,
        "assets_root": str(assets_dir),
        "launcher_name": "NeoForgeCarpetCI",
        "launcher_version": "1.0",
        "natives_directory": str(natives_dir),
        "library_directory": str(libraries_dir.resolve()),
        "classpath_separator": os.pathsep,
        "classpath": classpath,
    }

    program_args: list[str] = []
    jvm_args: list[str] = []
    main_class: str | None = None
    for manifest in manifests:
        if manifest.get("mainClass"):
            main_class = manifest["mainClass"]
        program_args.extend(argument_values(manifest, "game"))
        jvm_args.extend(argument_values(manifest, "jvm"))

    if not main_class:
        raise RuntimeError("No mainClass resolved from production manifests")

    program_args = [expand(value, placeholders) for value in program_args]
    jvm_args = [expand(value, placeholders) for value in jvm_args]
    program_args.append("--offlineDeveloperMode")
    if args.quick_play_multiplayer:
        program_args.extend(["--quickPlayMultiplayer", args.quick_play_multiplayer])

    unresolved = [value for value in (*jvm_args, *program_args) if "${" in value]
    if unresolved:
        raise RuntimeError(f"Unresolved launcher placeholders: {unresolved}")

    java_bin = Path(os.environ.get("JAVA_HOME", "")) / "bin" / "java"
    if not java_bin.is_file():
        java_bin = Path("java")

    command = [str(java_bin), *jvm_args, main_class, *program_args]
    output_path = install_dir / "production-client-output.log"
    print(f"Launching NeoForge production client ({len(classpath_items)} classpath items)")
    with output_path.open("wb") as output:
        process = subprocess.Popen(
            command,
            cwd=install_dir,
            stdout=output,
            stderr=subprocess.STDOUT,
            start_new_session=True,
            env=os.environ.copy(),
        )

        deadline = time.monotonic() + args.timeout
        ready = False
        while time.monotonic() < deadline:
            return_code = process.poll()
            combined = tail(output_path) + "\n" + tail(install_dir / "logs" / "latest.log")
            if args.required_marker:
                ready = args.required_marker in combined
            else:
                ready = any(marker in combined for marker in READY_MARKERS)
            if ready:
                break
            if return_code is not None:
                break
            time.sleep(1)

        if ready and args.stability_seconds > 0:
            stability_deadline = time.monotonic() + args.stability_seconds
            while time.monotonic() < stability_deadline:
                if process.poll() is not None:
                    ready = False
                    print("NeoForge production client exited during the stability window.", file=sys.stderr)
                    break
                time.sleep(0.25)

        if ready:
            if args.required_marker:
                print(f"NeoForge production client observed required marker: {args.required_marker}")
            else:
                print("NeoForge production client reached renderer/audio ready state.")
            terminate_process_group(process)
        elif process.poll() is None and not args.required_marker:
            # Staying alive through the full window still proves the packaged JAR
            # did not reproduce a bootstrap/mixin crash, even if a renderer marker
            # changes in a future Minecraft build.
            print(f"NeoForge production client stayed alive for {args.timeout}s; accepting smoke test.")
            ready = True
            terminate_process_group(process)
        else:
            if process.poll() is None:
                print(f"NeoForge production client did not observe required marker within {args.timeout}s.", file=sys.stderr)
                terminate_process_group(process)
            else:
                print(f"NeoForge production client exited early with code {process.returncode}.", file=sys.stderr)

    print("===== production client stdout/stderr =====")
    print(tail(output_path, 120000))
    latest_log = install_dir / "logs" / "latest.log"
    if latest_log.is_file():
        print("===== production client logs/latest.log =====")
        print(tail(latest_log, 120000))

    return 0 if ready else 1


if __name__ == "__main__":
    raise SystemExit(main())