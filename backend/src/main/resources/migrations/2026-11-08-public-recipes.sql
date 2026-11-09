-- Product decision: existing private recipes become public. Group visibility is preserved.
UPDATE recipe SET visibility='public' WHERE visibility='private';
ALTER TABLE recipe ALTER COLUMN visibility SET DEFAULT 'public';
