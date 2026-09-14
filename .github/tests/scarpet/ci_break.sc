// Runs against the packaged NeoForge JAR with the CI fake player.
__config() -> {'scope' -> 'global'};

global_cancel = false;
global_calls = 0;

__on_player_breaks_block(p, b) -> (
    if(query(p, 'name') == 'CarpetSmoke' && pos(b) == [12, 100, 0],
        global_calls += 1;
        if(global_cancel, 'cancel', null)
    )
);

test_breaks(mode) -> (
    p = player('CarpetSmoke');
    global_calls = 0;
    global_cancel = true;
    set(12, 100, 0, 'stone');
    before = str(inventory_get(p, 0));
    cancelled_result = harvest(p, 12, 100, 0);
    preserved = !cancelled_result
        && block(12, 100, 0) == 'stone'
        && str(inventory_get(p, 0)) == before
        && global_calls == 1;

    global_cancel = false;
    allowed_result = harvest(p, 12, 100, 0);
    allowed = allowed_result && block(12, 100, 0) == 'air'
        && global_calls == 2;
    tool_ok = if(mode == 'SURVIVAL',
        inventory_get(p, 0) == null,
        str(inventory_get(p, 0)) == before
    );
    if(preserved && allowed && tool_ok,
        run('say CARPET_BREAK_' + mode + '_OK'),
        run('say CARPET_BREAK_FAILED_' + mode
            + '_preserved=' + preserved + '_allowed=' + allowed + '_tool=' + tool_ok)
    );
    preserved && allowed && tool_ok
);
