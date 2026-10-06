package me.uc.hussein.ultrasclans.database;

/**
 * يحتوي على أوامر SQL لإنشاء الجداول (idempotent - IF NOT EXISTS).
 * الأعمدة مصممة لتغطية احتياجات المراحل القادمة أيضًا (Bank, CP,
 * Storage, Warps...) حتى لا نحتاج migration جذري لاحقًا، لكن المنطق
 * الفعلي لتلك الأعمدة يُفعّل تباعًا في الخدمات (Services) الخاصة بها.
 */
final class Schema {

    private Schema() {
    }

    static final String SQLITE_SCHEMA = """
        CREATE TABLE IF NOT EXISTS uc_clans (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            name_lower TEXT NOT NULL UNIQUE,
            owner_uuid TEXT NOT NULL,
            color TEXT NOT NULL DEFAULT 'WHITE',
            is_public INTEGER NOT NULL DEFAULT 1,
            member_capacity INTEGER NOT NULL DEFAULT 10,
            bank_balance REAL NOT NULL DEFAULT 0,
            bank_capacity REAL NOT NULL DEFAULT 50000,
            cp INTEGER NOT NULL DEFAULT 0,
            clan_level INTEGER NOT NULL DEFAULT 1,
            storage_level INTEGER NOT NULL DEFAULT 1,
            warp_capacity INTEGER NOT NULL DEFAULT 1,
            alliance_capacity INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS uc_clan_ranks (
            rank_uid INTEGER PRIMARY KEY AUTOINCREMENT,
            clan_id TEXT NOT NULL,
            rank_key TEXT NOT NULL,
            name TEXT NOT NULL,
            display TEXT NOT NULL,
            color TEXT NOT NULL,
            priority INTEGER NOT NULL,
            is_protected INTEGER NOT NULL DEFAULT 0,
            is_default INTEGER NOT NULL DEFAULT 0,
            permissions TEXT NOT NULL DEFAULT '',
            UNIQUE(clan_id, rank_key)
        );

        CREATE TABLE IF NOT EXISTS uc_clan_members (
            uuid TEXT PRIMARY KEY,
            clan_id TEXT NOT NULL,
            rank_key TEXT NOT NULL,
            member_level INTEGER NOT NULL DEFAULT 1,
            member_xp INTEGER NOT NULL DEFAULT 0,
            kills INTEGER NOT NULL DEFAULT 0,
            deaths INTEGER NOT NULL DEFAULT 0,
            contribution INTEGER NOT NULL DEFAULT 0,
            joined_at INTEGER NOT NULL,
            last_online INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS uc_clan_invites (
            id TEXT PRIMARY KEY,
            clan_id TEXT NOT NULL,
            sender_uuid TEXT NOT NULL,
            receiver_uuid TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            status TEXT NOT NULL DEFAULT 'PENDING'
        );

        CREATE TABLE IF NOT EXISTS uc_clan_requests (
            id TEXT PRIMARY KEY,
            clan_id TEXT NOT NULL,
            requester_uuid TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            status TEXT NOT NULL DEFAULT 'PENDING'
        );

        CREATE TABLE IF NOT EXISTS uc_member_history (
            uuid TEXT PRIMARY KEY,
            clan_id TEXT NOT NULL,
            clan_name TEXT NOT NULL,
            rank_key TEXT NOT NULL,
            member_level INTEGER NOT NULL,
            member_xp INTEGER NOT NULL,
            kills INTEGER NOT NULL,
            deaths INTEGER NOT NULL,
            contribution INTEGER NOT NULL,
            left_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS uc_player_cooldowns (
            uuid TEXT PRIMARY KEY,
            last_leave_at INTEGER NOT NULL DEFAULT 0,
            last_create_attempt_at INTEGER NOT NULL DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS uc_audit_logs (
            log_id INTEGER PRIMARY KEY AUTOINCREMENT,
            correlation_id TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            actor_uuid TEXT,
            clan_id TEXT,
            action TEXT NOT NULL,
            amount TEXT,
            before_value TEXT,
            after_value TEXT,
            source TEXT
        );

        CREATE TABLE IF NOT EXISTS uc_clan_storage (
            clan_id TEXT NOT NULL,
            slot_index INTEGER NOT NULL,
            item_data TEXT,
            PRIMARY KEY (clan_id, slot_index)
        );

        CREATE TABLE IF NOT EXISTS uc_mining_progress (
            uuid TEXT NOT NULL,
            material TEXT NOT NULL,
            count INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (uuid, material)
        );

        CREATE TABLE IF NOT EXISTS uc_clan_warps (
            clan_id TEXT NOT NULL,
            warp_id INTEGER NOT NULL,
            world TEXT NOT NULL,
            x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
            yaw REAL NOT NULL, pitch REAL NOT NULL,
            created_by TEXT,
            created_at INTEGER NOT NULL,
            PRIMARY KEY (clan_id, warp_id)
        );

        CREATE TABLE IF NOT EXISTS uc_clan_spawns (
            clan_id TEXT PRIMARY KEY,
            world TEXT NOT NULL,
            x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
            yaw REAL NOT NULL, pitch REAL NOT NULL
        );

        CREATE TABLE IF NOT EXISTS uc_player_settings (
            uuid TEXT PRIMARY KEY,
            clan_chat INTEGER NOT NULL DEFAULT 0,
            tag_enabled INTEGER NOT NULL DEFAULT 1
        );

        CREATE TABLE IF NOT EXISTS uc_clan_alliances (
            id TEXT PRIMARY KEY,
            clan_a TEXT NOT NULL,
            clan_b TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS uc_alliance_settings (
            alliance_id TEXT NOT NULL,
            granter_clan_id TEXT NOT NULL,
            allow_spawn INTEGER NOT NULL DEFAULT 0,
            allow_warp INTEGER NOT NULL DEFAULT 0,
            allow_storage INTEGER NOT NULL DEFAULT 0,
            allow_bank INTEGER NOT NULL DEFAULT 0,
            allow_member_list INTEGER NOT NULL DEFAULT 0,
            allow_clan_info INTEGER NOT NULL DEFAULT 0,
            prevent_pvp INTEGER NOT NULL DEFAULT 1,
            PRIMARY KEY (alliance_id, granter_clan_id)
        );

        CREATE TABLE IF NOT EXISTS uc_alliance_invites (
            id TEXT PRIMARY KEY,
            sender_clan_id TEXT NOT NULL,
            target_clan_id TEXT NOT NULL,
            sender_player TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            status TEXT NOT NULL DEFAULT 'PENDING'
        );

        CREATE TABLE IF NOT EXISTS uc_clan_tasks (
            clan_id TEXT NOT NULL,
            period TEXT NOT NULL,
            period_key INTEGER NOT NULL,
            progress INTEGER NOT NULL DEFAULT 0,
            claimed INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (clan_id, period)
        );

        CREATE TABLE IF NOT EXISTS uc_clan_events (
            id TEXT PRIMARY KEY,
            clan_a TEXT NOT NULL,
            clan_b TEXT NOT NULL,
            status TEXT NOT NULL,
            location_type TEXT NOT NULL,
            round_duration_seconds INTEGER NOT NULL,
            total_rounds INTEGER NOT NULL,
            current_round INTEGER NOT NULL DEFAULT 0,
            score_a INTEGER NOT NULL DEFAULT 0,
            score_b INTEGER NOT NULL DEFAULT 0,
            money_wager REAL NOT NULL DEFAULT 0,
            cp_wager INTEGER NOT NULL DEFAULT 0,
            winner_clan_id TEXT,
            created_by TEXT,
            created_at INTEGER NOT NULL,
            started_at INTEGER,
            ended_at INTEGER
        );

        CREATE TABLE IF NOT EXISTS uc_event_participants (
            event_id TEXT NOT NULL,
            player_uuid TEXT NOT NULL,
            clan_id TEXT NOT NULL,
            state TEXT NOT NULL,
            pre_world TEXT, pre_x REAL, pre_y REAL, pre_z REAL, pre_yaw REAL, pre_pitch REAL,
            disconnected_at INTEGER,
            PRIMARY KEY (event_id, player_uuid)
        );

        CREATE TABLE IF NOT EXISTS uc_admin_arena (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            event1_world TEXT, event1_x REAL, event1_y REAL, event1_z REAL, event1_yaw REAL, event1_pitch REAL,
            event2_world TEXT, event2_x REAL, event2_y REAL, event2_z REAL, event2_yaw REAL, event2_pitch REAL,
            rest1_world TEXT, rest1_x REAL, rest1_y REAL, rest1_z REAL,
            rest2_world TEXT, rest2_x REAL, rest2_y REAL, rest2_z REAL
        );

        CREATE TABLE IF NOT EXISTS uc_player_prefs (
            uuid TEXT PRIMARY KEY,
            language TEXT NOT NULL DEFAULT '',
            messages INTEGER NOT NULL DEFAULT 1,
            sounds INTEGER NOT NULL DEFAULT 1,
            hud INTEGER NOT NULL DEFAULT 1,
            accept_invites INTEGER NOT NULL DEFAULT 1,
            accept_tpa INTEGER NOT NULL DEFAULT 1,
            accept_alliance INTEGER NOT NULL DEFAULT 1,
            accept_join INTEGER NOT NULL DEFAULT 1,
            accept_challenges INTEGER NOT NULL DEFAULT 1
        );

        CREATE INDEX IF NOT EXISTS idx_uc_clan_members_clan ON uc_clan_members(clan_id);
        CREATE INDEX IF NOT EXISTS idx_uc_clan_ranks_clan ON uc_clan_ranks(clan_id);
        CREATE INDEX IF NOT EXISTS idx_uc_clan_invites_receiver ON uc_clan_invites(receiver_uuid, status);
        CREATE INDEX IF NOT EXISTS idx_uc_clan_invites_clan ON uc_clan_invites(clan_id, status);
        CREATE INDEX IF NOT EXISTS idx_uc_clan_requests_clan ON uc_clan_requests(clan_id, status);
        CREATE INDEX IF NOT EXISTS idx_uc_audit_logs_clan ON uc_audit_logs(clan_id);
        """;

    static final String MYSQL_SCHEMA = """
        CREATE TABLE IF NOT EXISTS uc_clans (
            id VARCHAR(36) PRIMARY KEY,
            name VARCHAR(32) NOT NULL,
            name_lower VARCHAR(32) NOT NULL UNIQUE,
            owner_uuid VARCHAR(36) NOT NULL,
            color VARCHAR(32) NOT NULL DEFAULT 'WHITE',
            is_public TINYINT(1) NOT NULL DEFAULT 1,
            member_capacity INT NOT NULL DEFAULT 10,
            bank_balance DOUBLE NOT NULL DEFAULT 0,
            bank_capacity DOUBLE NOT NULL DEFAULT 50000,
            cp INT NOT NULL DEFAULT 0,
            clan_level INT NOT NULL DEFAULT 1,
            storage_level INT NOT NULL DEFAULT 1,
            warp_capacity INT NOT NULL DEFAULT 1,
            alliance_capacity INT NOT NULL DEFAULT 1,
            created_at BIGINT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_ranks (
            rank_uid INT AUTO_INCREMENT PRIMARY KEY,
            clan_id VARCHAR(36) NOT NULL,
            rank_key VARCHAR(32) NOT NULL,
            name VARCHAR(32) NOT NULL,
            display VARCHAR(64) NOT NULL,
            color VARCHAR(16) NOT NULL,
            priority INT NOT NULL,
            is_protected TINYINT(1) NOT NULL DEFAULT 0,
            is_default TINYINT(1) NOT NULL DEFAULT 0,
            permissions TEXT NOT NULL,
            UNIQUE KEY uniq_clan_rank (clan_id, rank_key)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_members (
            uuid VARCHAR(36) PRIMARY KEY,
            clan_id VARCHAR(36) NOT NULL,
            rank_key VARCHAR(32) NOT NULL,
            member_level INT NOT NULL DEFAULT 1,
            member_xp INT NOT NULL DEFAULT 0,
            kills INT NOT NULL DEFAULT 0,
            deaths INT NOT NULL DEFAULT 0,
            contribution INT NOT NULL DEFAULT 0,
            joined_at BIGINT NOT NULL,
            last_online BIGINT NOT NULL,
            INDEX idx_clan (clan_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_invites (
            id VARCHAR(36) PRIMARY KEY,
            clan_id VARCHAR(36) NOT NULL,
            sender_uuid VARCHAR(36) NOT NULL,
            receiver_uuid VARCHAR(36) NOT NULL,
            created_at BIGINT NOT NULL,
            expires_at BIGINT NOT NULL,
            status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
            INDEX idx_receiver (receiver_uuid, status),
            INDEX idx_clan (clan_id, status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_requests (
            id VARCHAR(36) PRIMARY KEY,
            clan_id VARCHAR(36) NOT NULL,
            requester_uuid VARCHAR(36) NOT NULL,
            created_at BIGINT NOT NULL,
            expires_at BIGINT NOT NULL,
            status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
            INDEX idx_clan (clan_id, status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_member_history (
            uuid VARCHAR(36) PRIMARY KEY,
            clan_id VARCHAR(36) NOT NULL,
            clan_name VARCHAR(32) NOT NULL,
            rank_key VARCHAR(32) NOT NULL,
            member_level INT NOT NULL,
            member_xp INT NOT NULL,
            kills INT NOT NULL,
            deaths INT NOT NULL,
            contribution INT NOT NULL,
            left_at BIGINT NOT NULL,
            expires_at BIGINT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_player_cooldowns (
            uuid VARCHAR(36) PRIMARY KEY,
            last_leave_at BIGINT NOT NULL DEFAULT 0,
            last_create_attempt_at BIGINT NOT NULL DEFAULT 0
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_storage (
            clan_id VARCHAR(36) NOT NULL,
            slot_index INT NOT NULL,
            item_data MEDIUMTEXT,
            PRIMARY KEY (clan_id, slot_index)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_mining_progress (
            uuid VARCHAR(36) NOT NULL,
            material VARCHAR(64) NOT NULL,
            count INT NOT NULL DEFAULT 0,
            PRIMARY KEY (uuid, material)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_warps (
            clan_id VARCHAR(36) NOT NULL,
            warp_id INT NOT NULL,
            world VARCHAR(64) NOT NULL,
            x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL,
            yaw FLOAT NOT NULL, pitch FLOAT NOT NULL,
            created_by VARCHAR(36),
            created_at BIGINT NOT NULL,
            PRIMARY KEY (clan_id, warp_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_spawns (
            clan_id VARCHAR(36) PRIMARY KEY,
            world VARCHAR(64) NOT NULL,
            x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL,
            yaw FLOAT NOT NULL, pitch FLOAT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_player_settings (
            uuid VARCHAR(36) PRIMARY KEY,
            clan_chat TINYINT(1) NOT NULL DEFAULT 0,
            tag_enabled TINYINT(1) NOT NULL DEFAULT 1
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_alliances (
            id VARCHAR(36) PRIMARY KEY,
            clan_a VARCHAR(36) NOT NULL,
            clan_b VARCHAR(36) NOT NULL,
            created_at BIGINT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_alliance_settings (
            alliance_id VARCHAR(36) NOT NULL,
            granter_clan_id VARCHAR(36) NOT NULL,
            allow_spawn TINYINT(1) NOT NULL DEFAULT 0,
            allow_warp TINYINT(1) NOT NULL DEFAULT 0,
            allow_storage TINYINT(1) NOT NULL DEFAULT 0,
            allow_bank TINYINT(1) NOT NULL DEFAULT 0,
            allow_member_list TINYINT(1) NOT NULL DEFAULT 0,
            allow_clan_info TINYINT(1) NOT NULL DEFAULT 0,
            prevent_pvp TINYINT(1) NOT NULL DEFAULT 1,
            PRIMARY KEY (alliance_id, granter_clan_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_alliance_invites (
            id VARCHAR(36) PRIMARY KEY,
            sender_clan_id VARCHAR(36) NOT NULL,
            target_clan_id VARCHAR(36) NOT NULL,
            sender_player VARCHAR(36) NOT NULL,
            created_at BIGINT NOT NULL,
            expires_at BIGINT NOT NULL,
            status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_tasks (
            clan_id VARCHAR(36) NOT NULL,
            period VARCHAR(16) NOT NULL,
            period_key BIGINT NOT NULL,
            progress BIGINT NOT NULL DEFAULT 0,
            claimed TINYINT(1) NOT NULL DEFAULT 0,
            PRIMARY KEY (clan_id, period)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_clan_events (
            id VARCHAR(36) PRIMARY KEY,
            clan_a VARCHAR(36) NOT NULL,
            clan_b VARCHAR(36) NOT NULL,
            status VARCHAR(24) NOT NULL,
            location_type VARCHAR(24) NOT NULL,
            round_duration_seconds INT NOT NULL,
            total_rounds INT NOT NULL,
            current_round INT NOT NULL DEFAULT 0,
            score_a INT NOT NULL DEFAULT 0,
            score_b INT NOT NULL DEFAULT 0,
            money_wager DOUBLE NOT NULL DEFAULT 0,
            cp_wager BIGINT NOT NULL DEFAULT 0,
            winner_clan_id VARCHAR(36),
            created_by VARCHAR(36),
            created_at BIGINT NOT NULL,
            started_at BIGINT,
            ended_at BIGINT
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_event_participants (
            event_id VARCHAR(36) NOT NULL,
            player_uuid VARCHAR(36) NOT NULL,
            clan_id VARCHAR(36) NOT NULL,
            state VARCHAR(24) NOT NULL,
            pre_world VARCHAR(64), pre_x DOUBLE, pre_y DOUBLE, pre_z DOUBLE, pre_yaw FLOAT, pre_pitch FLOAT,
            disconnected_at BIGINT,
            PRIMARY KEY (event_id, player_uuid)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_admin_arena (
            id INT PRIMARY KEY,
            event1_world VARCHAR(64), event1_x DOUBLE, event1_y DOUBLE, event1_z DOUBLE, event1_yaw FLOAT, event1_pitch FLOAT,
            event2_world VARCHAR(64), event2_x DOUBLE, event2_y DOUBLE, event2_z DOUBLE, event2_yaw FLOAT, event2_pitch FLOAT,
            rest1_world VARCHAR(64), rest1_x DOUBLE, rest1_y DOUBLE, rest1_z DOUBLE,
            rest2_world VARCHAR(64), rest2_x DOUBLE, rest2_y DOUBLE, rest2_z DOUBLE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_player_prefs (
            uuid VARCHAR(36) PRIMARY KEY,
            language VARCHAR(8) NOT NULL DEFAULT '',
            messages TINYINT(1) NOT NULL DEFAULT 1,
            sounds TINYINT(1) NOT NULL DEFAULT 1,
            hud TINYINT(1) NOT NULL DEFAULT 1,
            accept_invites TINYINT(1) NOT NULL DEFAULT 1,
            accept_tpa TINYINT(1) NOT NULL DEFAULT 1,
            accept_alliance TINYINT(1) NOT NULL DEFAULT 1,
            accept_join TINYINT(1) NOT NULL DEFAULT 1,
            accept_challenges TINYINT(1) NOT NULL DEFAULT 1
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE TABLE IF NOT EXISTS uc_audit_logs (
            log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
            correlation_id VARCHAR(36) NOT NULL,
            timestamp BIGINT NOT NULL,
            actor_uuid VARCHAR(36),
            clan_id VARCHAR(36),
            action VARCHAR(64) NOT NULL,
            amount VARCHAR(64),
            before_value TEXT,
            after_value TEXT,
            source VARCHAR(64),
            INDEX idx_clan (clan_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
}
