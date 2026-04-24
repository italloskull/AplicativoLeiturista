import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    const { email, password, full_name, username, cargo } = await req.json()

    // Cliente com a SERVICE_ROLE (chave mestra)
    // As variáveis SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY são injetadas automaticamente pelo Supabase
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

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

    return new Response(JSON.stringify({ message: 'Usuário criado com sucesso!', user: userData.user }), {
      headers: { 'Content-Type': 'application/json' }
    })

  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
    })
  }
})
