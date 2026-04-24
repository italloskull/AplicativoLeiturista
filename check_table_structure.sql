-- Verificar estrutura da tabela customers
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'customers'
ORDER BY ordinal_position;

-- Verificar se há dados na tabela
SELECT COUNT(*) as total_records FROM customers;

-- Verificar um registro de exemplo (se existir)
SELECT * FROM customers LIMIT 1;

-- Verificar índices da tabela
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'customers';
