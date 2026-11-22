Atualização do **Muzix** com polimento visual das letras, animação palavra por palavra mais suave e melhorias de limpeza/alinhamento nas traduções.

### Correções e melhorias
- **Card de Letras mais limpo:** removidas as linhas de descrição do rodapé (*Palavra por palavra*, *Letras fornecidas por...* e origem da tradução), liberando mais espaço para os versos.
- **Animação palavra por palavra (`RICH_SYNCED`) suave e sem pisca-pisca:** o relógio de sincronização agora é estritamente monotônico (não recua sob pequenas variações de tempo do player) e o preenchimento branco por palavra ocorre direto na fase de desenho com varredura contínua, eliminando o efeito de "ir e voltar" em vocais rápidos.
- **Remoção de cabeçalhos bilíngues dentro da letra:** expandida a limpeza para reconhecer títulos e artistas com parênteses/kanji (como `少女A (Young Girl A) - 椎名もた (ぽわぽわP) / 鏡音鈴 (鏡音リン)`).
- **Traduções sem buracos e sem pontuação solta no final:**
  - Exige cobertura mínima de 85%–88% linha a linha e divide automaticamente pares humanos que uniam dois versos em uma única linha, evitando linhas em branco na tradução.
  - Remove `,`, `;` e `.` isolados no final das frases traduzidas (preservando `?`, `!` e `...`).
  - Cache de traduções renovado automaticamente (`#v4`).
- **Bloqueio reforçado de legendas quebradas (mesmo sem tags `[Muzică]`):** identifica e rejeita legendas fragmentadas/fonéticas (`.`, `am`, `me f`, `de`, `soha`), limpando automaticamente entradas antigas salvas no modo offline.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.9/LICENSE
