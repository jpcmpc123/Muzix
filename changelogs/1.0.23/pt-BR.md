Atualização do **Muzix** com capa instantânea no player em tela cheia, loading coeso de página inteira em Álbuns e Playlists e pré-carregamento completo das faixas da playlist (sem spinner no rodapé ao rolar).

### Principais Melhorias (`v1.0.23`)
- **Capa Instantânea no Player em Tela Cheia:**
  - Corrige o bug do Coil 3 em que `placeholder = rememberHolderPainter()` sobrescrevia incondicionalmente a capa já em RAM (`placeholderMemoryCacheKey`) pela logo do Muzix até o download de `800px` terminar.
  - Elimina o flash intermediário de `thumbnailURL = null` no `SharedViewModel` e adiciona fallback para `songEntity.thumbnails` quando `artworkUri` vier ausente.
  - Pré-aquece em segundo plano a capa `800px` no `MiniPlayer` assim que a faixa começa a tocar.
- **Loading Inicial Coeso e Playlist 100% Carregada:**
  - **Álbuns e Playlists (online e locais):** mantêm a tela de loading inicial (com botão Voltar) até que metadados, estado local (`liked`/`downloadState`), lista de faixas, capa hero (`800px`) e cor do fundo (`paletteState`) estejam prontos, revelando a página final pronta de uma só vez.
  - **Fim do spinner ao rolar playlists para baixo:** playlists online carregam automaticamente as páginas de continuação já durante o loading inicial (com inserção em banco em segundo plano) e playlists locais usam janela estendida de `PagingConfig`, removendo o spinner gigante do rodapé da lista.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.23/LICENSE
