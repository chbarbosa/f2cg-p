CREATE TABLE IF NOT EXISTS players (
    id                       VARCHAR(36)   PRIMARY KEY,
    username                 VARCHAR(100)  NOT NULL UNIQUE,
    password_hash            VARCHAR(60)   NOT NULL,
    active                   BOOLEAN       NOT NULL DEFAULT FALSE,
    activation_code          VARCHAR(5),
    activation_code_expires  TIMESTAMP,
    nickname                 VARCHAR(20),
    country                  VARCHAR(2)
);

CREATE TABLE IF NOT EXISTS cards (
    id           VARCHAR(36)  PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    mana_cost    INT          NOT NULL,
    card_type    VARCHAR(10)  NOT NULL,
    theme        VARCHAR(10)  NOT NULL,
    unit_class   VARCHAR(10),
    attack       INT,
    defense      INT,
    effect_type  VARCHAR(20),
    effect_value INT
);

CREATE TABLE IF NOT EXISTS decks (
    id          VARCHAR(36)   PRIMARY KEY,
    player_id   VARCHAR(36)   NOT NULL REFERENCES players(id),
    name        VARCHAR(100)  NOT NULL,
    theme       VARCHAR(10)   NOT NULL,
    card_ids    VARCHAR(1000) NOT NULL DEFAULT '',
    status      VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS queue_entries (
    id          VARCHAR(36)  PRIMARY KEY,
    player_id   VARCHAR(36)  NOT NULL REFERENCES players(id),
    deck_id     VARCHAR(36)  NOT NULL REFERENCES decks(id),
    status      VARCHAR(10)  NOT NULL DEFAULT 'WAITING',
    joined_at   TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS seasons (
    id                       VARCHAR(36)  PRIMARY KEY,
    season_year              INT          NOT NULL,
    season_number            INT          NOT NULL,
    name                     VARCHAR(100),
    start_date               DATE         NOT NULL,
    end_date                 DATE         NOT NULL,
    phase2_start_date        DATE         NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    last_weekly_calculation  DATE
);

CREATE TABLE IF NOT EXISTS player_season_stats (
    id                VARCHAR(36)  PRIMARY KEY,
    player_id         VARCHAR(36)  NOT NULL REFERENCES players(id),
    season_id         VARCHAR(36)  NOT NULL REFERENCES seasons(id),
    total_matches     INT          NOT NULL DEFAULT 0,
    victories         INT          NOT NULL DEFAULT 0,
    defeats           INT          NOT NULL DEFAULT 0,
    rank              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    highest_rank      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    matches_this_week INT          NOT NULL DEFAULT 0,
    last_rank_update  TIMESTAMP
);