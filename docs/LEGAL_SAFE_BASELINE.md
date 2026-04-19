# Baseline legal e tecnico (clean-room)

Objetivo: atingir paridade funcional com referencia externa sem copiar codigo literal.

## Regras obrigatorias

- Nao copiar/colar arquivos, funcoes, classes, comentarios ou textos do projeto de referencia.
- Nao reutilizar nomes internos exclusivos quando eles nao forem necessarios.
- Descrever apenas comportamento esperado (entrada, saida, estado, erro, performance).
- Reimplementar do zero no DeepMule com arquitetura propria.
- Registrar origem da ideia como "referencia comportamental" quando necessario.

## Politica de PR

Cada PR deve declarar:

- Escopo funcional implementado.
- Evidencia de implementacao original no DeepMule.
- Lista de testes executados.
- Confirmacao: sem copia literal de codigo/texto.

## O que pode ser usado como referencia

- Fluxo de usuario (ex.: biblioteca -> jogo -> emulacao).
- Criterios de UX e desempenho.
- Lista de features de alto nivel.

## O que nao pode ser copiado literalmente

- Codigo fonte e arquivos de build.
- Assets graficos sem licenca compativel.
- Textos/documentacao com copyright sem permissao explicita.

