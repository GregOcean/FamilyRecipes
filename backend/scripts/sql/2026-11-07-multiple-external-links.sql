-- Apply once to the existing family_recipes database before starting the updated backend.
-- Existing rows keep their original data and fall back to id order.
ALTER TABLE external_recipe
  ADD COLUMN notes VARCHAR(2000) NULL,
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
