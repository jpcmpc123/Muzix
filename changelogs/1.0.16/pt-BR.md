Atualização do **Muzix** eliminando sobras de vocalizações (`", oh"`, `"yeah"`) e conectivos deslocados entre linhas nas traduções de músicas.

### Correções
- **Filtro de "Tradução automática" do Letras.mus.br:** páginas do Letras sem tradutor humano (marcadas como *Tradução automática*) agora são ignoradas, permitindo que o app use traduções humanas do Vagalume ou o tradutor neural com contexto de estrofe aplicado diretamente na letra sincronizada.
- **Limpeza de vocalizações extras nas bordas das frases:** quando a base humana inclui uma vocalização no fim ou começo do verso (como `", oh"`, `"yeah"`, `"uh"`, `"ah"`, `"whoa"`) que não existe na linha sincronizada exibida na tela, essa sobra é removida automaticamente da tradução.
- **Rebalanceamento de conectivos entre versos adjacentes:** quando uma palavra de ligação (como `"when"` / `"quando"`, `"and"` / `"e"`, `"but"` / `"mas"`, `"because"` / `"porque"`, `"if"` / `"se"`, `"while"` / `"enquanto"`) fica presa no final da linha de cima na tradução humana enquanto a letra sincronizada a coloca no início da linha de baixo, ela é movida automaticamente para a frase correta.
- **Atualização automática do cache de traduções (`#v8`):** recarrega músicas anteriormente cacheadas sem precisar limpar dados do aplicativo.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.16/LICENSE
