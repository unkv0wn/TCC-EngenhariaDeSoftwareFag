# Dashboard Homepage — Design Spec

Data: 2026-07-29
Escopo: `FrontEnd-Next/` (Next.js)

## Contexto

O `FrontEnd-Next` está sendo construído do zero, desconectado do backend por enquanto
(ver `services/auth.ts`). Já existe a tela de login (`app/login`). Esta spec cobre a
próxima tela: a "homepage" do app, que é a **tela pós-login** (dashboard/hub), não uma
landing page pública de marketing.

Design validado interativamente com o usuário via mockups (companion visual), com
paleta e componentes já existentes no login como base.

## Decisões de escopo

- **O que é a homepage:** dashboard pós-login, rota própria (`/dashboard`), separada de
  `/login`. Não é landing page — a ideia de página de marketing estilo Stripe/Vercel/Linear
  (conteúdo tipo hero, "como funciona", features, etc.) foi descartada; usamos só a
  **estética** desses produtos (clean, whitespace, tipografia grande) aplicada ao dashboard.
- **Autenticação:** sem guarda de rota por enquanto — `/dashboard` fica acessível
  diretamente por URL, sem checar sessão. Fora de escopo desta spec: navegação real do
  login para o dashboard (depende de `services/auth.ts`, hoje desconectado do backend).
- **Dados dos KPIs:** puramente demonstrativos (mock estático no componente), sem
  integração com o backend agora.

## Layout

Duas regiões: sidebar fixa à esquerda + área de conteúdo principal.

### Sidebar (224px)

De cima para baixo:
1. Marca: ícone quadrado roxo (`bg-primary-600`) com "R" + wordmark "RouteWise".
2. Navegação principal: Dashboard (ativo), Nova rota, Minhas rotas — ícone Lucide + label,
   item ativo com fundo `primary-50` e texto `primary-700`.
3. Rodapé (empurrado pro fim via `margin-top: auto`): divider (`border-top` sutil),
   depois "Configurações" e "Sair" — mesmo estilo visual dos itens de nav; "Sair" ganha
   `hover` em tom de `danger` (fundo `danger-50`, texto `danger-600`).

Não há mais bloco de avatar/e-mail do usuário no rodapé.

### Área principal

1. Header: título "Dashboard" + subtítulo "Visão geral das suas rotas", com botão
   primário "Nova rota" (ícone `Plus`) alinhado à direita.
2. **KPI row** — grid de 4 stat tiles (card branco, borda `gray-200`, radius 10px):
   - ícone num chip 32px (`bg-primary-50`, `text-primary-600`)
   - valor grande (24px, peso 800)
   - label abaixo (12.5px, `gray-500`, peso 600)

   Tiles (mock estático): Rotas criadas (12) · Distância total (184 km) · Tempo médio
   por rota (34 min) · Pontos otimizados (58). Sem delta/tendência — números ilustrativos.
3. Abaixo do KPI: **em aberto**. Não faz parte desta spec; um placeholder tracejado
   neutro ocupa o espaço até a próxima iteração de design.

## Estilo visual

- Paleta: tokens já existentes em `app/globals.css` (`primary-*` roxo, `gray-*` neutro).
  Nenhuma nova cor introduzida.
- Fundo da área principal: `gray-50` (`#f9fafb`); sidebar e cards em branco.
- Tipografia: **Nunito** substitui a fonte atual do body (que hoje cai em fallback
  Arial/Helvetica — `globals.css` nunca aplicava de fato a variável do Geist). Nunito
  carregada via `next/font/google`, mesmo padrão já usado para Geist em `layout.tsx`.
- Ícones: `lucide-react` (já é dependência do projeto) substituindo os emojis usados
  nos mockups iniciais.

## Componentes a criar

- `components/dashboard/Sidebar.tsx` — nav + rodapé, conforme layout acima.
- `components/dashboard/KpiTile.tsx` — tile individual (props: `icon`, `value`, `label`).
- `components/dashboard/KpiRow.tsx` — grid dos 4 tiles com os dados mock.
- `app/dashboard/page.tsx` — compõe sidebar + header + KpiRow + placeholder.

## Fora de escopo (explícito)

- Conteúdo abaixo do KPI row.
- Guarda de rota / redirecionamento real pós-login.
- Dados reais dos KPIs (viria de um futuro endpoint agregando rotas do usuário).
- Landing page pública de marketing (pode virar uma spec separada no futuro).
