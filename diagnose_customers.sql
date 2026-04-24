-- Verificar se a tabela customers existe
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'customers';

-- Verificar quantos clientes existem
SELECT COUNT(*) as total_customers FROM customers;

-- Verificar as políticas RLS da tabela customers
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename = 'customers';

-- Verificar se RLS está habilitado na tabela
SELECT schemaname, tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'customers';

-- Verificar alguns registros de exemplo (se existirem)
SELECT id, name, registration_number, created_at
FROM customers
LIMIT 5;
