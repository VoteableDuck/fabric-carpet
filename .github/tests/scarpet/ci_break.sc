// Exercise player actions outside /script run, which suppresses events.
__config() -> {'scope' -> 'global'};
global_cancel = false;
global_calls = 0;
global_before = '';
global_mode = '';

__on_player_breaks_block(p, b) -> (
    if(query(p, 'name') == 'CarpetSmoke' && pos(b) == [12, 100, 0],
        global_calls += 1;
        run('say CARPET_BREAK_ATTEMPT_' + global_mode + '_' + if(global_cancel, 'CANCEL', 'ALLOW'));
        if(global_cancel, 'cancel', null)
    )
);

prepare(mode) -> (
    global_mode = mode;
    global_calls = 0;
    global_cancel = true;
    set(12, 100, 0, 'stone');
    global_before = str(inventory_get(player('CarpetSmoke'), 0))
);

check_cancel() -> (
    preserved = block(12, 100, 0) == 'stone'
        && str(inventory_get(player('CarpetSmoke'), 0)) == global_before
        && global_calls == 1;
    run('say CARPET_BREAK_' + if(preserved, global_mode + '_CANCEL_OK', 'FAILED_CANCEL_' + global_mode));
    global_cancel = false;
    preserved
);

check_allow() -> (
    p = player('CarpetSmoke');
    allowed = block(12, 100, 0) == 'air' && global_calls == 2;
    tool_ok = if(global_mode == 'SURVIVAL',
        inventory_get(p, 0) == null,
        str(inventory_get(p, 0)) == global_before
    );
    run('say CARPET_BREAK_' + if(allowed && tool_ok, global_mode + '_OK',
        'FAILED_ALLOW_' + global_mode + '_allowed=' + allowed + '_tool=' + tool_ok));
    allowed && tool_ok
);
