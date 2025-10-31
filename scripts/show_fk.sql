SELECT table_name,
       constraint_name
FROM information_schema.table_constraints
WHERE table_schema = 'campus_market_opt'
  AND constraint_type = 'FOREIGN KEY'
ORDER BY table_name, constraint_name;
