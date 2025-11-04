-- SQLite schema for the E-Voting System

BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS users (
  username TEXT PRIMARY KEY,
  display_name TEXT,
  role TEXT,
  verified INTEGER
);

CREATE TABLE IF NOT EXISTS candidates (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE,
  manifesto TEXT,
  votes INTEGER
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT
);

CREATE TABLE IF NOT EXISTS voted_users (
  username TEXT PRIMARY KEY
);

-- Seed demo data
INSERT OR IGNORE INTO candidates(name, manifesto, votes) VALUES
 ('Alice','Transparency and Innovation',0),
 ('Bob','Community and Growth',0);

INSERT OR IGNORE INTO users(username, display_name, role, verified) VALUES
 ('voter1','Voter One','VOTER',0),
 ('admin','Administrator','ADMIN',1);

INSERT OR IGNORE INTO settings(key, value) VALUES('electionActive','false');

COMMIT;


