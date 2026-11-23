Atualização do **Muzix** corrigindo traduções com palavras repetidas artificialmente (como `"maluca e maluca"` para *"wacky crazy"*) e ampliando o suporte às traduções humanas do Letras.mus.br.

### Correções e Melhorias
- **Correção automática de palavras duplicadas artificiais no tradutor neural:** quando o tradutor primário transforma dois adjetivos diferentes do original (como `"wacky crazy"`, `"silly goofy"`, `"weird and strange"`) em repetições artificiais (`"maluca e maluca"`, `"boba e boba"`, `"estranho e estranho"`), o app consulta uma segunda opinião pontual e aplica substituição por sinônimos naturais em português ou colapso limpo — mantendo intactas repetições reais da letra (como `"rich, rich"` → `"muito, muito rico"`).
- **Suporte conservador a traduções humanas do Letras.mus.br em 2 colunas:** músicas do Letras que não usam o modo empilhado por terem uma estrofe extra no fim (como *BeVerly Hills BoYfRiEnd*) agora têm suas estrofes pareadas com segurança, evitando depender apenas da tradução automática.
- **Atualização automática do cache de traduções (`#v9`):** atualiza músicas já cacheadas sem precisar limpar dados do aplicativo.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.17/LICENSE
