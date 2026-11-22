Atualização do **Muzix** focada em **limpeza automática de letras** e **traduções muito mais naturais em Português do Brasil (`pt-BR`)**.

### Correções e melhorias
- **Remoção de cabeçalhos e créditos dentro da letra:**
  - Corrigido o problema em que algumas músicas exibiam linhas como **`Nome da Música - Artista`** ou créditos técnicos (*Lyrics by*, *Written by*, *Synced by*, etc.) no início/fim da letra, contaminando também a faixa compacta acima do título e a tradução.
- **Traduções mais naturais e compreensíveis (`pt-BR`):**
  - **Traduções humanas do Vagalume (`pt-BR`):** antes de traduzir automaticamente, o app busca traduções humanas brasileiras no Vagalume e alinha verso a verso com a sincronia original (mantendo também `SimpMusic Letras` e legendas humanas nativas do YouTube).
  - **Correção do motor neural da Microsoft Edge (`pt-BR`):** atualizado para o novo endpoint público sem necessidade de token de autenticação (corrigindo a falha que forçava todas as músicas da `v1.0.5` para o Google Tradutor linha a linha), com detecção real de versos quebrados (*enjambement*) e normalização de gírias/contrações cantadas em inglês (`gonna`, `'cause`, `ain't`, expressões idiomáticas).
  - **Modo IA opcional aprimorado:** modelo padrão do Gemini atualizado para `gemini-2.5-flash` com prompt poético/idiomático para quem optar por usar chave própria.
  - **Cache atualizado + Retraduzir:** traduções antigas da `v1.0.5` são atualizadas automaticamente ao tocar em `文A`, e **pressionar e segurar** o botão `文A` força uma nova tradução.
  - **Origem discreta no rodapé:** mostra de qual fonte veio a tradução (*Tradução humana · Vagalume*, *Tradução neural · Microsoft*, etc.).

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.6/LICENSE
