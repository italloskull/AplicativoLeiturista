import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    const { email, password, full_name, username, cargo, cidade_id, cidades } = await req.json()

    // Cliente com a SERVICE_ROLE (chave mestra)
    // As variáveis SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY são injetadas automaticamente pelo Supabase
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const normalizedRole = (cargo ?? '').toString().trim().toLowerCase()
    const roleRequiresSingleCity = normalizedRole === 'usuário' || normalizedRole === 'usuario'
    const incomingCities = Array.isArray(cidades) ? cidades : []
    const normalizedCityIds = Array.from(
      new Set(
        [
          ...incomingCities,
          cidade_id
        ]
          .map((value) => (value ?? '').toString().trim())
          .filter((value) => value.length > 0)
      )
    )

    if (normalizedCityIds.length === 0) {
      return new Response(JSON.stringify({ error: 'Pelo menos uma cidade deve ser informada para o usuário.' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const cityIdsToPersist = roleRequiresSingleCity ? normalizedCityIds.slice(0, 1) : normalizedCityIds

    // 1. Cria o usuário no Auth (sem deslogar o admin)
    const { data: userData, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: { full_name, username }
    })

    if (authError) {
        return new Response(JSON.stringify({ error: authError.message }), {
            status: 400,
            headers: { 'Content-Type': 'application/json' }
        })
    }

    // 2. Insere/Atualiza o perfil na tabela 'profiles'
    // Nota: Dependendo da sua trigger, o perfil pode já ter sido criado, então usamos upsert
    const { error: profileError } = await supabaseAdmin
      .from('profiles')
      .upsert({
          id: userData.user.id,
          email: email,
          full_name: full_name,
          username: username,
          cargo: cargo
      })

    if (profileError) {
        return new Response(JSON.stringify({ error: profileError.message }), {
            status: 400,
            headers: { 'Content-Type': 'application/json' }
        })
    }

    // 3. Persiste vínculos de cidade de forma idempotente
    const cityRelationsPayload = cityIdsToPersist.map((cityId) => ({
      usuario_id: userData.user.id,
      cidade_id: cityId
    }))

    const { error: cityRelationError } = await supabaseAdmin
      .from('usuario_cidades')
      .upsert(cityRelationsPayload, { onConflict: 'usuario_id,cidade_id', ignoreDuplicates: true })

    if (cityRelationError) {
      return new Response(JSON.stringify({ error: `Perfil criado, mas falhou ao vincular cidades: ${cityRelationError.message}` }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    return new Response(JSON.stringify({
      message: 'Usuário criado com sucesso!',
      user: userData.user,
      cidades_vinculadas: cityIdsToPersist.length
    }), {
      headers: { 'Content-Type': 'application/json' }
    })

  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
    })
  }
})
