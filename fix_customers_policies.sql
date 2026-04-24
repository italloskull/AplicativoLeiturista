-- Habilitar RLS na tabela customers (se não estiver habilitado)
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- Remover políticas existentes que podem estar causando problemas
DROP POLICY IF EXISTS "customers_select" ON customers;
DROP POLICY IF EXISTS "customers_insert" ON customers;
DROP POLICY IF EXISTS "customers_update" ON customers;
DROP POLICY IF EXISTS "customers_delete" ON customers;

-- Criar políticas seguras para customers
-- Permitir que usuários autenticados vejam todos os clientes (para fins de demonstração)
CREATE POLICY "customers_select" ON customers
FOR SELECT
TO authenticated
USING (true);

-- Permitir que usuários autenticados insiram clientes
CREATE POLICY "customers_insert" ON customers
FOR INSERT
TO authenticated
WITH CHECK (true);

-- Permitir que usuários autenticados atualizem clientes
CREATE POLICY "customers_update" ON customers
FOR UPDATE
TO authenticated
USING (true)
WITH CHECK (true);

-- Permitir que usuários autenticados excluam clientes
CREATE POLICY "customers_delete" ON customers
FOR DELETE
TO authenticated
USING (true);

-- Verificar as políticas criadas
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename = 'customers';
