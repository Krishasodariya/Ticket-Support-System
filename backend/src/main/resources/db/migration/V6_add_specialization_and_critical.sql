-- Aufgabe 15: Agent-Spezialisierung
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);

-- Aufgabe 39: Ticket als kritisch markieren
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS is_critical BOOLEAN NOT NULL DEFAULT FALSE;