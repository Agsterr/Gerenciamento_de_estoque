export interface HelpPrerequisite {
  label: string;
  route: string;
}

export interface TutorialStep {
  order: number;
  title: string;
  description: string;
  route: string;
  icon: string;
  prerequisites: HelpPrerequisite[];
  tips: string[];
}

export interface ModuleHelp {
  id: string;
  title: string;
  icon: string;
  route: string;
  summary: string;
  prerequisites: HelpPrerequisite[];
  howTo: string[];
  tips?: string[];
}

export const TUTORIAL_STEPS: TutorialStep[] = [
  {
    order: 1,
    title: 'Categorias',
    description: 'Comece cadastrando as categorias dos seus produtos (ex.: Bebidas, Eletrônicos, Limpeza). Todo produto precisa estar vinculado a uma categoria.',
    route: '/dashboard/categorias',
    icon: 'fa-list-alt',
    prerequisites: [],
    tips: ['Use nomes claros e objetivos.', 'Você pode criar quantas categorias precisar antes de cadastrar produtos.'],
  },
  {
    order: 2,
    title: 'Produtos',
    description: 'Com as categorias prontas, cadastre seus produtos com nome, preço, quantidade em estoque e estoque mínimo.',
    route: '/dashboard/produtos',
    icon: 'fa-box',
    prerequisites: [{ label: 'Categorias', route: '/dashboard/categorias' }],
    tips: ['SKU e código de barras são opcionais.', 'O estoque mínimo ajuda a identificar produtos com pouca quantidade.'],
  },
  {
    order: 3,
    title: 'Consumidores',
    description: 'Cadastre os clientes ou destinatários que receberão entregas. Necessário para registrar saídas por entrega.',
    route: '/dashboard/consumidores',
    icon: 'fa-users',
    prerequisites: [],
    tips: ['O CPF deve ter 11 dígitos e ser único na organização.'],
  },
  {
    order: 4,
    title: 'Fornecedores e Depósitos',
    description: 'Cadastre fornecedores para compras e depósitos para organizar o estoque por local. Podem ser feitos em qualquer ordem.',
    route: '/dashboard/fornecedores',
    icon: 'fa-truck-loading',
    prerequisites: [],
    tips: [
      'Fornecedores são obrigatórios para pedidos de compra.',
      'Depósitos são obrigatórios para inventário; em pedidos de compra são opcionais.',
    ],
  },
  {
    order: 5,
    title: 'Entregas',
    description: 'Registre entregas de produtos para consumidores. Cada entrega gera automaticamente uma movimentação de saída no estoque.',
    route: '/dashboard/entregas',
    icon: 'fa-truck',
    prerequisites: [
      { label: 'Produtos', route: '/dashboard/produtos' },
      { label: 'Consumidores', route: '/dashboard/consumidores' },
    ],
    tips: ['O entregador é o usuário logado no sistema.'],
  },
  {
    order: 6,
    title: 'Pedidos de Compra',
    description: 'Crie pedidos para repor estoque com fornecedores. Ao receber o pedido, o estoque dos produtos é atualizado.',
    route: '/dashboard/pedidos-compra',
    icon: 'fa-shopping-cart',
    prerequisites: [
      { label: 'Fornecedores', route: '/dashboard/fornecedores' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
    tips: ['Adicione pelo menos um item (produto + quantidade + preço) em cada pedido.'],
  },
  {
    order: 7,
    title: 'Inventário',
    description: 'Faça contagens físicas por depósito para conferir e ajustar o estoque registrado no sistema.',
    route: '/dashboard/inventario',
    icon: 'fa-clipboard-check',
    prerequisites: [
      { label: 'Depósitos', route: '/dashboard/depositos' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
    tips: ['Os itens da contagem são gerados automaticamente com base no depósito selecionado.'],
  },
  {
    order: 8,
    title: 'Relatórios e Auditoria',
    description: 'Consulte relatórios de movimentação e o histórico de ações registradas automaticamente pelo sistema.',
    route: '/dashboard/relatorios',
    icon: 'fa-chart-bar',
    prerequisites: [],
    tips: ['A auditoria registra alterações feitas nos módulos do sistema.'],
  },
];

export const MODULES_HELP: ModuleHelp[] = [
  {
    id: 'categorias',
    title: 'Categorias',
    icon: 'fa-list-alt',
    route: '/dashboard/categorias',
    summary: 'Organize seus produtos em grupos. É o primeiro passo do cadastro.',
    prerequisites: [],
    howTo: [
      'Clique em "Nova Categoria".',
      'Informe o nome e salve.',
      'Repita para todas as categorias necessárias antes de cadastrar produtos.',
    ],
  },
  {
    id: 'produtos',
    title: 'Produtos',
    icon: 'fa-box',
    route: '/dashboard/produtos',
    summary: 'Cadastro central do estoque. Depende de ao menos uma categoria.',
    prerequisites: [{ label: 'Categorias', route: '/dashboard/categorias' }],
    howTo: [
      'Clique em "Novo".',
      'Preencha nome, descrição, preço, quantidade e estoque mínimo.',
      'Selecione a categoria do produto.',
      'Salve para incluir na lista.',
    ],
    tips: ['Use a busca por código de barras na listagem quando disponível.'],
  },
  {
    id: 'consumidores',
    title: 'Consumidores',
    icon: 'fa-users',
    route: '/dashboard/consumidores',
    summary: 'Clientes ou destinatários das entregas.',
    prerequisites: [],
    howTo: [
      'Clique em "Novo".',
      'Informe nome, CPF (11 dígitos) e endereço.',
      'Salve o cadastro.',
    ],
  },
  {
    id: 'fornecedores',
    title: 'Fornecedores',
    icon: 'fa-truck-loading',
    route: '/dashboard/fornecedores',
    summary: 'Empresas ou pessoas de quem você compra produtos.',
    prerequisites: [],
    howTo: [
      'Clique em "Novo".',
      'Informe pelo menos o nome (CNPJ, e-mail e telefone são opcionais).',
      'Salve para usar em pedidos de compra.',
    ],
  },
  {
    id: 'depositos',
    title: 'Depósitos',
    icon: 'fa-warehouse',
    route: '/dashboard/depositos',
    summary: 'Locais físicos onde o estoque é armazenado.',
    prerequisites: [],
    howTo: [
      'Clique em "Novo".',
      'Informe o nome do depósito.',
      'Marque como padrão se for o principal da organização.',
    ],
    tips: ['Necessário para inventário. Opcional em pedidos de compra.'],
  },
  {
    id: 'entregas',
    title: 'Entregas',
    icon: 'fa-truck',
    route: '/dashboard/entregas',
    summary: 'Saída de produtos para consumidores com registro automático de movimentação.',
    prerequisites: [
      { label: 'Produtos', route: '/dashboard/produtos' },
      { label: 'Consumidores', route: '/dashboard/consumidores' },
    ],
    howTo: [
      'Abra o formulário de nova entrega.',
      'Selecione o produto e o consumidor.',
      'Informe a quantidade e confirme.',
    ],
  },
  {
    id: 'movimentacoes',
    title: 'Movimentações',
    icon: 'fa-exchange-alt',
    route: '/dashboard/movimentacoes',
    summary: 'Histórico e registro manual de entradas e saídas de estoque.',
    prerequisites: [{ label: 'Produtos', route: '/dashboard/produtos' }],
    howTo: [
      'Use os filtros para localizar movimentações por período ou tipo.',
      'Para registrar manualmente, abra o modal e informe o produto, tipo e quantidade.',
    ],
    tips: ['Entregas e recebimentos de pedidos geram movimentações automaticamente.'],
  },
  {
    id: 'pedidos-compra',
    title: 'Pedidos de Compra',
    icon: 'fa-shopping-cart',
    route: '/dashboard/pedidos-compra',
    summary: 'Compras de reposição com fornecedores.',
    prerequisites: [
      { label: 'Fornecedores', route: '/dashboard/fornecedores' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
    howTo: [
      'Clique em "Novo Pedido".',
      'Selecione o fornecedor (e depósito, se desejar).',
      'Adicione itens com produto, quantidade e preço unitário.',
      'Salve e, quando o material chegar, marque como recebido.',
    ],
  },
  {
    id: 'inventario',
    title: 'Inventário',
    icon: 'fa-clipboard-check',
    route: '/dashboard/inventario',
    summary: 'Contagem física de estoque por depósito.',
    prerequisites: [
      { label: 'Depósitos', route: '/dashboard/depositos' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
    howTo: [
      'Clique em "Nova Contagem".',
      'Selecione o depósito.',
      'Informe as quantidades contadas para cada item.',
      'Finalize a contagem para ajustar o estoque.',
    ],
  },
  {
    id: 'relatorios',
    title: 'Relatórios',
    icon: 'fa-chart-bar',
    route: '/dashboard/relatorios',
    summary: 'Consultas e exportação de dados de movimentação.',
    prerequisites: [],
    howTo: [
      'Defina o período e o tipo de movimentação desejado.',
      'Visualize os resultados e exporte se necessário.',
    ],
    tips: ['Funciona melhor após haver movimentações registradas no sistema.'],
  },
  {
    id: 'auditoria',
    title: 'Auditoria',
    icon: 'fa-history',
    route: '/dashboard/auditoria',
    summary: 'Registro automático de ações realizadas no sistema (somente leitura).',
    prerequisites: [],
    howTo: [
      'Navegue pela lista de logs.',
      'Use os filtros para localizar ações por data ou módulo.',
    ],
  },
  {
    id: 'usuarios',
    title: 'Usuários',
    icon: 'fa-user-cog',
    route: '/dashboard/usuarios',
    summary: 'Gestão de usuários da organização (ativar/desativar). Novos usuários entram pelo cadastro público.',
    prerequisites: [],
    howTo: [
      'Novos membros devem se cadastrar em "Cadastro" na página inicial.',
      'Administradores podem ativar ou desativar usuários existentes.',
    ],
  },
  {
    id: 'assinatura',
    title: 'Assinatura',
    icon: 'fa-credit-card',
    route: '/assinatura',
    summary: 'Plano e pagamento do sistema. Necessário para manter o acesso após o período de teste.',
    prerequisites: [],
    howTo: [
      'Acesse pelo menu do usuário ou pelo dashboard.',
      'Escolha um plano e conclua o pagamento.',
    ],
  },
];

export const PAGE_HINTS: Record<string, { message: string; prerequisites: HelpPrerequisite[] }> = {
  categorias: {
    message: 'Primeiro passo: cadastre as categorias antes de criar produtos. Elas organizam seu catálogo e são obrigatórias no cadastro de produto.',
    prerequisites: [],
  },
  produtos: {
    message: 'Cadastre o produto (nome, preço, categoria). Estoque inicial é opcional na criação. Depois, repor via Pedidos de Compra ou Movimentações; sair via Entregas ou Movimentações.',
    prerequisites: [{ label: 'Categorias', route: '/dashboard/categorias' }],
  },
  consumidores: {
    message: 'Cadastre os clientes ou destinatários que receberão entregas. Não depende de outros cadastros.',
    prerequisites: [],
  },
  fornecedores: {
    message: 'Cadastre fornecedores para usar em pedidos de compra. Apenas o nome é obrigatório.',
    prerequisites: [],
  },
  depositos: {
    message: 'Depósitos guardam a quantidade física por local. Entregas e movimentações atualizam o depósito padrão automaticamente.',
    prerequisites: [],
  },
  'pedidos-venda': {
    message: 'Venda externa: cliente, pagamento e preços. Retirada interna: funcionário, sem pagamento — só produto e quantidade. Ao confirmar, baixa estoque.',
    prerequisites: [
      { label: 'Clientes', route: '/dashboard/consumidores' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
  },
  entregas: {
    message: 'Use Pedidos de Venda (menu Vendas) para registrar saída de material para clientes.',
    prerequisites: [{ label: 'Vendas', route: '/dashboard/pedidos-venda' }],
  },
  movimentacoes: {
    message: 'Histórico de entradas e saídas. Por padrão mostra tudo. Entregas geram saída; receber pedido de compra gera entrada.',
    prerequisites: [{ label: 'Produtos', route: '/dashboard/produtos' }],
  },
  'pedidos-compra': {
    message: 'Compra de produtos do fornecedor. Cada item do pedido é um produto cadastrado. Ao receber, entra estoque no depósito e gera movimentação de entrada.',
    prerequisites: [
      { label: 'Fornecedores', route: '/dashboard/fornecedores' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
  },
  inventario: {
    message: 'Abaixo você vê o estoque atual dos produtos. Para conferir fisicamente e ajustar quantidades, cadastre um depósito e clique em "Nova Contagem".',
    prerequisites: [
      { label: 'Depósitos', route: '/dashboard/depositos' },
      { label: 'Produtos', route: '/dashboard/produtos' },
    ],
  },
};
