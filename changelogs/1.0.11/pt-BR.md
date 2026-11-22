Atualização do **Muzix** com novos controles nas letras, ocultação inteligente do botão de tradução, letras estáticas de reserva via Letras.mus.br e correção de sincronização em faixas com timestamps deslocados.

### Novidades e correções
- **Alternar destaque palavra por palavra ou frase inteira:** nova opção **Destaque palavra por palavra** em *Configurações → Letras* para escolher entre iluminar cada palavra cantada (quando a música tiver sincronia por palavra) ou iluminar a frase inteira em todas as músicas.
- **Botão de tradução oculto no idioma do app:** músicas cuja letra já está em português (ou no idioma configurado no app/tradução) não exibem mais o botão de tradução nem disparam tradução automática desnecessária.
- **Fallback de letra estática pelo Letras.mus.br:** quando os provedores sincronizados não encontram letra para uma música (e ela não é instrumental confirmada), o app busca a letra original no Letras.mus.br para exibição estática (sem efeitos de tempo/frase).
- **Correção de sincronização e trechos cortados (ex.: *Aham — Massaru*):** detecção de timestamps que ultrapassam a duração real da faixa no LRCLIB, SimpMusic e cache local, substituindo automaticamente versões deslocadas (ex.: LRC de clipe mais longo em faixa de 02:21) pela versão sincronizada correta e completa.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.11/LICENSE
