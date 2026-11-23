Atualização do **Muzix** garantindo que **Escolhas rápidas** carregue e apareça sempre em primeiro lugar na aba Início antes das outras seções.

### Correções e Melhorias
- **Escolhas rápidas carregando por primeiro no Início:** quando o YouTube envia *Escolhas rápidas / Escolha a dedo* em páginas seguintes da resposta inicial, o repositório agora busca automaticamente as primeiras páginas necessárias antes de liberar o carregamento inicial e posiciona **Escolhas rápidas** no topo desde o primeiro quadro.
- **Prioridade total no carregamento inicial:** as seções do final da página (*Humores e gêneros*, *Novos lançamentos* e *Paradas*) passaram a carregar somente após o feed principal do Início resolver, evitando competição de rede na abertura do app.
- **Proteção contra ancoragem fora da tela:** mesmo quando *Escolhas rápidas* chegar por paginação tardia, a lista mantém a posição no topo (`index = 0`) em vez de deixar outras seções acima dela.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.18/LICENSE
