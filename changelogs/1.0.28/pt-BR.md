Atualização do **Muzix** com **som aberto e espacial (estilo Wavelet)** no Equalizador e carregamento de capa de álbuns/playlists em **`<= 0,5 s` nos dados móveis**.

### Principais Melhorias (`v1.0.28`)
- **Equalizador Aberto & Espacial (Estilo Wavelet):**
  - **Correção de Fase & Largura Estéreo:** corrigido o bug de polaridade (inversão de 180°) no filtro *Bass Mono* e movido o corte para um filtro Butterworth limpo em **`100 Hz`** — liberando 100% da amplitude estéreo nos médios, guitarras e vocais mantendo o sub-grave centralizado.
  - **Limiter Transparente (`-0,63 dBFS`):** o limiter agora só atua nos picos que realmente iriam distorcer (em vez de comprimir cada batida), devolvendo o fôlego e a abertura natural da música.
  - **Brilho & Clareza em `6 kHz` + `Auto Equilibrado` mais vivo:** agudos mais abertos e naturais nos fones Bluetooth (como o *Galaxy Buds Core*).
  - **Novo Controle `Espaço / Sala 3D` (`0%` – `100%`) + Preset `Palco Aberto / Spatial`:** micro-reflexões estéreo limpas (sem som de eco e sem afetar os graves) + **Palco Estéreo** ampliado até **`175%`**.
- **Capas de Álbuns e Playlists em `<= 0,5 s` nos Dados Móveis:**
  - **Pré-aquecimento no clique (`t = 0 ms`):** ao tocar em um card na Home, Biblioteca, Busca ou Artista, o app reaproveita instantaneamente a imagem já em memória e baixa a capa do topo em paralelo com a API.
  - **Banda 100% exclusiva para a capa do topo:** o pré-carregamento das miniaturas das faixas e a paginação em segundo plano agora aguardam a capa principal aparecer (com resolução Hero de `600px` e miniaturas compactas no 4G), eliminando cancelamentos no meio do download e travamentos na abertura.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados, perfis salvos nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.28/LICENSE
