Atualização do **Muzix** com botão circular de **tradução de letras (`文A`) sob demanda**, tradução natural gratuita em **Português do Brasil (`pt-BR`)** sem necessidade de chave de API de IA e otimizações de desempenho no player de letras.

### Novidades e melhorias
- **Botão circular de tradução de letras (`文A` com badge `×` quando ativo):**
  - Adicionado na barra inferior da tela de **Letras em Tela Cheia** (acessível em todos os estilos de player) e no cabeçalho do card de letras do **Player Clássico**.
  - Alternar entre original e traduzido é instantâneo (`0 ms`) após o primeiro carregamento, mantendo a tradução em cache na memória e no banco local.
- **Tradução natural gratuita sem API de IA paga:**
  - **Prioridade em 4 camadas:** traduções humanas da comunidade (`SimpMusic Lyrics`, agora para qualquer provedor de letra) e legendas humanas nativas do YouTube → **Microsoft Edge Neural Translator em `pt-BR` com contexto de estrofe** (preservando sentido de versos quebrados e 100% da sincronia) → fallback automático para Google Tradutor em lote → opção de IA (Gemini/OpenAI) caso o usuário tenha sua própria chave.
  - Detecta automaticamente quando a letra já está no idioma de destino e avisa sem duplicar o texto.
- **Visual limpo nas letras clássicas:**
  - Substituído o texto traduzido em amarelo forte por **branco suave harmonizado** abaixo dos versos originais em negrito, melhorando a leitura.
- **Mais desempenho e economia de bateria:**
  - Tradução **sob demanda por padrão** (com opção em *Configurações → Letras → Traduzir letras automaticamente*), eliminando requisições HTTP ocultas disparadas em segundo plano a cada música.
  - Alinhamento das linhas traduzidas movido para fora da Main Thread (`Dispatchers.Default`) com cache LRU e fast-path `O(N)` sem ordenação.
  - Reutilização do cliente HTTP já existente, cache de token JWT, deduplicação de refrões repetidos e isolamento de recomposição para manter a rolagem fluida.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.5/LICENSE
