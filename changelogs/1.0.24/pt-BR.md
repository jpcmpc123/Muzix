Atualização do **Muzix** com abertura ultrarrápida de Álbuns e Playlists: músicas e miniaturas das faixas começam a carregar primeiro, enquanto a capa principal no topo entra instantaneamente da RAM (ou em menos de 1 segundo com upgrade automático para alta resolução).

### Principais Melhorias (`v1.0.24`)
- **Abertura muito mais rápida em Playlists:**
  - Libera a página imediatamente assim que a 1ª página de músicas chega (~0,3–0,5 s) e carrega silenciosamente o restante das páginas de continuação em segundo plano — sem travar a tela de loading inicial e sem spinner no rodapé ao rolar para baixo.
- **Abertura muito mais rápida em Álbuns:**
  - Remove buscas HTTP seriais lentas por faixa durante a abertura de álbuns com videoclipes (`YouTube.album`), limitando o `next` auxiliar a `1 s` e deixando o upgrade OMV → ATV sob demanda na reprodução.
- **Prioridade para músicas + capas das faixas e capa principal no topo em ≤ 1 s:**
  - Pré-carrega imediatamente as capas das primeiras 16 músicas (`240px`) assim que a lista chega e preserva capas em RAM nas linhas de faixa (`SongFullWidthItems`).
  - Reaproveita em **0 ms** qualquer capa de álbum/playlist já em memória RAM (Home, Busca ou Biblioteca) para exibir a capa + cor do fundo instantaneamente, fazendo o upgrade para `800px` em paralelo; quando ainda não estiver em RAM, usa fast-pass `400px` com teto de `450 ms` + upgrade `800px` em paralelo (capa pronta no topo em ≤ 1 s).

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.24/LICENSE
