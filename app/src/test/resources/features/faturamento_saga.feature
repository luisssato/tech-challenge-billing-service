# language: pt
Funcionalidade: Faturamento, Orçamento e Pagamento da Ordem de Serviço
  Como microsserviço de billing da oficina
  Quero processar diagnósticos, gerar orçamentos, viabilizar pagamentos e executar compensações
  Para garantir a consistência transacional da saga distribuída

  Cenário: Fluxo feliz - Diagnóstico gera orçamento, aprovação inicia pagamento e webhook confirma
    Dado que existe o tipo de peça com ID 1, descrição "Óleo Motor" e valor 50.00
    E que existe o tipo de serviço com ID 1, descrição "Troca de Óleo" e valor 100.00
    Quando o evento DIAGNOSTICO_CONCLUIDO chega para a OS 2001 com 2 unidades da peça 1 e 1 unidade do serviço 1
    Então o orçamento da OS 2001 fica "GERADO" com valor total 200.00
    Quando o evento ORCAMENTO_APROVADO chega para a OS 2001
    Então o orçamento da OS 2001 fica "APROVADO"
    E a cobrança de pagamento é criada com status "PENDENTE" e QR Code disponível
    Quando a notificação de pagamento aprovado é processada para a OS 2001
    Então o pagamento da OS 2001 fica "APROVADO"

  Cenário: Compensação - Falha na execução após pagamento aciona estorno
    Dado que existe a OS 2002 com pagamento "APROVADO" no valor de 200.00
    Quando o evento EXECUCAO_FALHOU chega para a OS 2002 com motivo "SEM_ESTOQUE"
    Então o pagamento da OS 2002 fica "ESTORNADO"

  Cenário: Cancelamento - Cancelamento da OS antes do pagamento encerra o orçamento
    Dado que existe o tipo de peça com ID 1, descrição "Óleo Motor" e valor 50.00
    E que existe o tipo de serviço com ID 1, descrição "Troca de Óleo" e valor 100.00
    Quando o evento DIAGNOSTICO_CONCLUIDO chega para a OS 2003 com 1 unidades da peça 1 e 1 unidade do serviço 1
    Então o orçamento da OS 2003 fica "GERADO" com valor total 150.00
    Quando o evento OS_CANCELADA chega para a OS 2003 com motivo "DESISTENCIA_CLIENTE"
    Então o orçamento da OS 2003 fica "REJEITADO"
