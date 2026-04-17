-- ============================================================
-- TEST PLAYER: test01@gmail.com / pass123
-- ============================================================

-- Player (active, no pending activation)
INSERT INTO players (id, username, password_hash, active, nickname, country) VALUES
('aaaaaaaa-0001-0000-0000-000000000008', 'test01@gmail.com',
 '$2b$10$aHTGws9W3WlsdU7wMW/izeIrr6fEcG2wzRK7zqKNaYcrsRioJKgF.',
 TRUE, 'TestPlayer01', 'BR');

-- Seasons
INSERT INTO seasons (id, season_year, season_number, name, start_date, end_date, phase2_start_date, status) VALUES
-- Active season (S1 2026) — currently
('bbbbbbbb-0001-0000-0000-000000000007', 2026, 2, 'S2 2026',
 '2026-03-01', '2026-04-30', '2026-04-01', 'ACTIVE'),
('bbbbbbbb-0001-0000-0000-000000000006', 2026, 1, 'S1 2026',
 '2026-01-01', '2026-02-28', '2026-02-01', 'FINISHED'),
('bbbbbbbb-0001-0000-0000-000000000005', 2025, 5, 'S5 2025',
 '2025-09-01', '2025-10-30', '2025-10-01', 'FINISHED'),
('bbbbbbbb-0001-0000-0000-000000000004', 2025, 4, 'S4 2025',
 '2025-07-01', '2025-08-31', '2025-08-01', 'FINISHED'),
('bbbbbbbb-0001-0000-0000-000000000003', 2025, 3, 'S3 2025',
 '2025-05-01', '2025-06-30', '2025-06-01', 'FINISHED'),
('bbbbbbbb-0001-0000-0000-000000000002', 2025, 2, 'S2 2025',
 '2025-03-01', '2025-04-30', '2025-04-01', 'FINISHED'),
('bbbbbbbb-0001-0000-0000-000000000001', 2025, 1, 'S1 2025',
 '2025-01-01', '2025-03-30', '2025-02-01', 'FINISHED');

-- Player season stats (player_id = aaaaaaaa-0001-0000-0000-000000000008)
INSERT INTO player_season_stats
    (id, player_id, season_id, total_matches, victories, defeats, rank, highest_rank, matches_this_week)
VALUES
-- S2 2026 (active, FREE phase): ROOKIE start, 3 matches so far
('cccccccc-0001-0000-0000-000000000007',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000007',
 3, 2, 1, 'PENDING', 'PENDING', 3),
-- S1 2026 (finished, RANKED): ADVANCED, peak ELITE, 8 matches that week
('cccccccc-0001-0000-0000-000000000006',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000006',
 45, 28, 17, 'ADVANCED', 'ELITE', 8),
-- S5 2025 (finished): ELITE rank achieved
('cccccccc-0001-0000-0000-000000000005',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000005',
 52, 35, 17, 'ELITE', 'ELITE', 0),
-- S4 2025 (finished): INTERMEDIATE, climbing
('cccccccc-0001-0000-0000-000000000004',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000004',
 38, 22, 16, 'INTERMEDIATE', 'ADVANCED', 0),
-- S3 2025 (finished): ROOKIE, first ranked season
('cccccccc-0001-0000-0000-000000000003',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000003',
 20, 10, 10, 'ROOKIE', 'ROOKIE', 0),
-- S2 2025 (finished): just started, PENDING
('cccccccc-0001-0000-0000-000000000002',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000002',
 8, 4, 4, 'PENDING', 'PENDING', 0),
-- S1 2025 (finished): very first season, few matches
('cccccccc-0001-0000-0000-000000000001',
 'aaaaaaaa-0001-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000001',
 5, 2, 3, 'PENDING', 'PENDING', 0);

-- ============================================================
-- TEST PLAYER: test02@gmail.com / pass123
-- ============================================================

INSERT INTO players (id, username, password_hash, active, nickname, country) VALUES
('aaaaaaaa-0002-0000-0000-000000000008', 'test02@gmail.com',
 '$2b$10$aHTGws9W3WlsdU7wMW/izeIrr6fEcG2wzRK7zqKNaYcrsRioJKgF.',
 TRUE, 'TestPlayer02', 'BR');

INSERT INTO player_season_stats
    (id, player_id, season_id, total_matches, victories, defeats, rank, highest_rank, matches_this_week)
VALUES
('cccccccc-0002-0000-0000-000000000007',
 'aaaaaaaa-0002-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000007',
 0, 0, 0, 'PENDING', 'PENDING', 0);

-- ============================================================
-- TEST PLAYER: test03@gmail.com / pass123
-- ============================================================

INSERT INTO players (id, username, password_hash, active, nickname, country) VALUES
('aaaaaaaa-0003-0000-0000-000000000008', 'test03@gmail.com',
 '$2b$10$aHTGws9W3WlsdU7wMW/izeIrr6fEcG2wzRK7zqKNaYcrsRioJKgF.',
 TRUE, 'TestPlayer03', 'BR');

INSERT INTO player_season_stats
    (id, player_id, season_id, total_matches, victories, defeats, rank, highest_rank, matches_this_week)
VALUES
('cccccccc-0003-0000-0000-000000000007',
 'aaaaaaaa-0003-0000-0000-000000000008',
 'bbbbbbbb-0001-0000-0000-000000000007',
 0, 0, 0, 'PENDING', 'PENDING', 0);

-- ============================================================

-- WARRIOR cards (35: 22 units, 7 buffs, 6 debuffs)
INSERT INTO cards (id, name, mana_cost, card_type, theme, unit_class, attack, defense, effect_type, effect_value) VALUES
('w-u-01', 'Iron Knight',      3, 'UNIT',   'WARRIOR', 'WARRIOR', 4, 5, NULL, NULL),
('w-u-02', 'Shield Bearer',    2, 'UNIT',   'WARRIOR', 'WARRIOR', 2, 6, NULL, NULL),
('w-u-03', 'Battle Axeman',    3, 'UNIT',   'WARRIOR', 'WARRIOR', 5, 3, NULL, NULL),
('w-u-04', 'Heavy Lancer',     4, 'UNIT',   'WARRIOR', 'WARRIOR', 6, 4, NULL, NULL),
('w-u-05', 'War Veteran',      4, 'UNIT',   'WARRIOR', 'WARRIOR', 5, 5, NULL, NULL),
('w-u-06', 'Berserker',        5, 'UNIT',   'WARRIOR', 'WARRIOR', 7, 2, NULL, NULL),
('w-u-07', 'Sword Saint',      5, 'UNIT',   'WARRIOR', 'WARRIOR', 6, 5, NULL, NULL),
('w-u-08', 'Iron Wall',        3, 'UNIT',   'WARRIOR', 'WARRIOR', 2, 7, NULL, NULL),
('w-u-09', 'Assault Trooper',  2, 'UNIT',   'WARRIOR', 'WARRIOR', 4, 2, NULL, NULL),
('w-u-10', 'Battle Priest',    4, 'UNIT',   'WARRIOR', 'WARRIOR', 4, 4, NULL, NULL),
('w-u-11', 'Warlord',          5, 'UNIT',   'WARRIOR', 'WARRIOR', 7, 4, NULL, NULL),
('w-u-12', 'Siege Master',     4, 'UNIT',   'WARRIOR', 'WARRIOR', 5, 4, NULL, NULL),
('w-u-13', 'Vanguard',         3, 'UNIT',   'WARRIOR', 'WARRIOR', 4, 3, NULL, NULL),
('w-u-14', 'Fortress Guard',   3, 'UNIT',   'WARRIOR', 'WARRIOR', 3, 6, NULL, NULL),
('w-u-15', 'Blade Dancer',     4, 'UNIT',   'WARRIOR', 'WARRIOR', 6, 3, NULL, NULL),
('w-u-16', 'Pike Soldier',     2, 'UNIT',   'WARRIOR', 'WARRIOR', 4, 2, NULL, NULL),
('w-u-17', 'Champion',         5, 'UNIT',   'WARRIOR', 'WARRIOR', 6, 6, NULL, NULL),
('w-u-18', 'Iron Fist',        3, 'UNIT',   'WARRIOR', 'WARRIOR', 5, 2, NULL, NULL),
('w-u-19', 'Wall Breaker',     4, 'UNIT',   'WARRIOR', 'WARRIOR', 7, 3, NULL, NULL),
('w-u-20', 'Oath Knight',      4, 'UNIT',   'WARRIOR', 'WARRIOR', 5, 5, NULL, NULL),
('w-b-01', 'War Cry',          2, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'ATK_BOOST', 3),
('w-b-02', 'Iron Resolve',     2, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'DEF_BOOST', 3),
('w-b-03', 'Charge',           3, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'ATK_BOOST', 4),
('w-b-04', 'Shield Wall',      2, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'DEF_BOOST', 3),
('w-b-05', 'Fury',             3, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'ATK_BOOST', 3),
('w-b-06', 'Steel Skin',       2, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'DEF_BOOST', 2),
('w-d-01', 'Weaken',           1, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'ATK_REDUCTION', 2),
('w-d-02', 'Armor Crack',      2, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'DEF_REDUCTION', 3),
('w-d-03', 'Demoralize',       2, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'ATK_REDUCTION', 2),
('w-d-04', 'Break Shield',     3, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'DEF_REDUCTION', 4),
('w-d-05', 'Cripple',          3, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'ATK_REDUCTION', 3),
('w-u-21', 'Steel Juggernaut', 5, 'UNIT',   'WARRIOR', 'WARRIOR', 8, 3, NULL, NULL),
('w-u-22', 'Sentinel',         2, 'UNIT',   'WARRIOR', 'WARRIOR', 3, 5, NULL, NULL),
('w-b-07', 'Battle Hymn',      2, 'BUFF',   'WARRIOR', NULL, NULL, NULL, 'ATK_BOOST', 2),
('w-d-06', 'Shatter',          2, 'DEBUFF', 'WARRIOR', NULL, NULL, NULL, 'DEF_REDUCTION', 2);

-- MAGE cards (35: 22 units, 7 buffs, 6 debuffs)
INSERT INTO cards (id, name, mana_cost, card_type, theme, unit_class, attack, defense, effect_type, effect_value) VALUES
('m-u-01', 'Fire Apprentice',  2, 'UNIT',   'MAGE', 'MAGE', 3, 2, NULL, NULL),
('m-u-02', 'Ice Witch',        3, 'UNIT',   'MAGE', 'MAGE', 3, 4, NULL, NULL),
('m-u-03', 'Storm Mage',       4, 'UNIT',   'MAGE', 'MAGE', 5, 3, NULL, NULL),
('m-u-04', 'Arcane Scholar',   3, 'UNIT',   'MAGE', 'MAGE', 4, 3, NULL, NULL),
('m-u-05', 'Flame Sorcerer',   4, 'UNIT',   'MAGE', 'MAGE', 6, 2, NULL, NULL),
('m-u-06', 'Frost Adept',      3, 'UNIT',   'MAGE', 'MAGE', 3, 5, NULL, NULL),
('m-u-07', 'Lightning Caster', 4, 'UNIT',   'MAGE', 'MAGE', 6, 3, NULL, NULL),
('m-u-08', 'Void Mage',        5, 'UNIT',   'MAGE', 'MAGE', 7, 3, NULL, NULL),
('m-u-09', 'Archmage',         5, 'UNIT',   'MAGE', 'MAGE', 6, 5, NULL, NULL),
('m-u-10', 'Pyromancer',       4, 'UNIT',   'MAGE', 'MAGE', 7, 2, NULL, NULL),
('m-u-11', 'Runeweaver',       3, 'UNIT',   'MAGE', 'MAGE', 4, 4, NULL, NULL),
('m-u-12', 'Chronomancer',     4, 'UNIT',   'MAGE', 'MAGE', 5, 4, NULL, NULL),
('m-u-13', 'Mirror Mage',      3, 'UNIT',   'MAGE', 'MAGE', 4, 3, NULL, NULL),
('m-u-14', 'Blizzard Caller',  4, 'UNIT',   'MAGE', 'MAGE', 5, 3, NULL, NULL),
('m-u-15', 'Ember Witch',      3, 'UNIT',   'MAGE', 'MAGE', 5, 2, NULL, NULL),
('m-u-16', 'Gale Wizard',      3, 'UNIT',   'MAGE', 'MAGE', 4, 3, NULL, NULL),
('m-u-17', 'Rift Walker',      5, 'UNIT',   'MAGE', 'MAGE', 6, 4, NULL, NULL),
('m-u-18', 'Shadow Caster',    4, 'UNIT',   'MAGE', 'MAGE', 6, 3, NULL, NULL),
('m-u-19', 'Spell Weaver',     3, 'UNIT',   'MAGE', 'MAGE', 4, 4, NULL, NULL),
('m-u-20', 'Arcane Sentinel',  4, 'UNIT',   'MAGE', 'MAGE', 5, 5, NULL, NULL),
('m-b-01', 'Arcane Surge',     2, 'BUFF',   'MAGE', NULL, NULL, NULL, 'ATK_BOOST', 3),
('m-b-02', 'Spellshield',      2, 'BUFF',   'MAGE', NULL, NULL, NULL, 'DEF_BOOST', 3),
('m-b-03', 'Empower',          3, 'BUFF',   'MAGE', NULL, NULL, NULL, 'ATK_BOOST', 4),
('m-b-04', 'Ley Line',         3, 'BUFF',   'MAGE', NULL, NULL, NULL, 'DEF_BOOST', 3),
('m-b-05', 'Overcharge',       3, 'BUFF',   'MAGE', NULL, NULL, NULL, 'ATK_BOOST', 3),
('m-b-06', 'Ethereal Boost',   2, 'BUFF',   'MAGE', NULL, NULL, NULL, 'DEF_BOOST', 2),
('m-d-01', 'Mana Drain',       2, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'ATK_REDUCTION', 2),
('m-d-02', 'Silence',          2, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'DEF_REDUCTION', 2),
('m-d-03', 'Arcane Shock',     3, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'ATK_REDUCTION', 3),
('m-d-04', 'Frostbite',        1, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'ATK_REDUCTION', 2),
('m-d-05', 'Nullify',          2, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'DEF_REDUCTION', 3),
('m-u-21', 'Astral Titan',     5, 'UNIT',   'MAGE', 'MAGE', 7, 4, NULL, NULL),
('m-u-22', 'Apprentice Scout', 2, 'UNIT',   'MAGE', 'MAGE', 3, 3, NULL, NULL),
('m-b-07', 'Spell Focus',      2, 'BUFF',   'MAGE', NULL, NULL, NULL, 'ATK_BOOST', 2),
('m-d-06', 'Mind Break',       2, 'DEBUFF', 'MAGE', NULL, NULL, NULL, 'DEF_REDUCTION', 2);

-- CLERIC cards (35: 22 units, 7 buffs, 6 debuffs)
INSERT INTO cards (id, name, mana_cost, card_type, theme, unit_class, attack, defense, effect_type, effect_value) VALUES
('c-u-01', 'Holy Knight',      3, 'UNIT',   'CLERIC', 'CLERIC', 4, 5, NULL, NULL),
('c-u-02', 'Temple Guard',     3, 'UNIT',   'CLERIC', 'CLERIC', 3, 6, NULL, NULL),
('c-u-03', 'Blessed Monk',     2, 'UNIT',   'CLERIC', 'CLERIC', 3, 4, NULL, NULL),
('c-u-04', 'Divine Healer',    3, 'UNIT',   'CLERIC', 'CLERIC', 2, 6, NULL, NULL),
('c-u-05', 'Paladin',          4, 'UNIT',   'CLERIC', 'CLERIC', 5, 5, NULL, NULL),
('c-u-06', 'Sacred Warrior',   4, 'UNIT',   'CLERIC', 'CLERIC', 6, 4, NULL, NULL),
('c-u-07', 'Light Bringer',    5, 'UNIT',   'CLERIC', 'CLERIC', 5, 6, NULL, NULL),
('c-u-08', 'Order Champion',   5, 'UNIT',   'CLERIC', 'CLERIC', 6, 5, NULL, NULL),
('c-u-09', 'Faithful Squire',  2, 'UNIT',   'CLERIC', 'CLERIC', 3, 3, NULL, NULL),
('c-u-10', 'Anointed Soldier', 3, 'UNIT',   'CLERIC', 'CLERIC', 4, 4, NULL, NULL),
('c-u-11', 'High Priest',      5, 'UNIT',   'CLERIC', 'CLERIC', 4, 7, NULL, NULL),
('c-u-12', 'Crusader',         4, 'UNIT',   'CLERIC', 'CLERIC', 5, 5, NULL, NULL),
('c-u-13', 'Sun Disciple',     3, 'UNIT',   'CLERIC', 'CLERIC', 4, 3, NULL, NULL),
('c-u-14', 'Radiant Monk',     3, 'UNIT',   'CLERIC', 'CLERIC', 3, 5, NULL, NULL),
('c-u-15', 'Warden of Faith',  4, 'UNIT',   'CLERIC', 'CLERIC', 4, 6, NULL, NULL),
('c-u-16', 'Altar Keeper',     2, 'UNIT',   'CLERIC', 'CLERIC', 2, 5, NULL, NULL),
('c-u-17', 'Beacon Knight',    5, 'UNIT',   'CLERIC', 'CLERIC', 6, 5, NULL, NULL),
('c-u-18', 'Sanctuary Guard',  3, 'UNIT',   'CLERIC', 'CLERIC', 3, 6, NULL, NULL),
('c-u-19', 'Devout Soldier',   2, 'UNIT',   'CLERIC', 'CLERIC', 3, 4, NULL, NULL),
('c-u-20', 'Chosen One',       5, 'UNIT',   'CLERIC', 'CLERIC', 6, 6, NULL, NULL),
('c-b-01', 'Bless',            1, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'DEF_BOOST', 2),
('c-b-02', 'Holy Light',       2, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'DEF_BOOST', 3),
('c-b-03', 'Divine Shield',    3, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'DEF_BOOST', 4),
('c-b-04', 'Inspire',          2, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'ATK_BOOST', 2),
('c-b-05', 'Consecrate',       3, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'ATK_BOOST', 3),
('c-b-06', 'Sacred Vow',       3, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'DEF_BOOST', 3),
('c-d-01', 'Smite',            2, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'ATK_REDUCTION', 3),
('c-d-02', 'Curse',            2, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'DEF_REDUCTION', 2),
('c-d-03', 'Holy Wrath',       3, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'ATK_REDUCTION', 3),
('c-d-04', 'Condemn',          2, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'DEF_REDUCTION', 3),
('c-d-05', 'Judgment',         3, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'ATK_REDUCTION', 2),
('c-u-21', 'Exarch',           5, 'UNIT',   'CLERIC', 'CLERIC', 5, 7, NULL, NULL),
('c-u-22', 'Novice Acolyte',   2, 'UNIT',   'CLERIC', 'CLERIC', 2, 4, NULL, NULL),
('c-b-07', 'Fortify',          2, 'BUFF',   'CLERIC', NULL, NULL, NULL, 'DEF_BOOST', 2),
('c-d-06', 'Enfeeble',         2, 'DEBUFF', 'CLERIC', NULL, NULL, NULL, 'ATK_REDUCTION', 2);