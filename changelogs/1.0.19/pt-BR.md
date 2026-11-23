Atualização do **Muzix** focada em carregamento instantâneo de capas e miniaturas e redução expressiva de tamanho do app e consumo de memória.

### Carregamento de Imagens Muito Mais Rápido
- **Motor de cache em disco sem cópia dupla (`NetworkFirstInterceptor`):** elimina gravação temporária redundante em misses puros, aumenta para **7 dias** o TTL das CDNs imutáveis de arte (`googleusercontent`, `ggpht`, `ytimg`) e atualiza em segundo plano com *stale-while-revalidate* sem travar a renderização.
- **Conexões e paralelismo Coil/OkHttp:** pool dedicado (`64` requisições simultâneas, `16` por host e timeouts enxutos de `8s`/`12s`), formato `RGB_565` no Android e teto de decodificação de `1280px` para reduzir uso de RAM em listas longas.
- **Miniaturas no tamanho certo + reutilização de RAM:** padronização de resoluções (`240px` em miniaturas/avatares, `400px` em cards, `800px` no player/hero, `360px` no glow e `128px` em fundos com desfoque), remoção de sufixos `+ "BIGGER"`, unificação da chave de cache entre telas e *crossfade* mais ágil (`120–150ms`).
- **Pré-carregamento inteligente:** prefetch automático das primeiras capas em *Escolhas rápidas* e das faixas anterior/próxima na fila do player.

### App Mais Leve (APK e Armazenamento)
- **Menos espaço no aparelho:** `jniLibs.useLegacyPackaging = false` evita extração redundante de bibliotecas nativas (~27 MB a menos ocupados após instalar).
- **APK enxuto:** remoção de codecs não usados no Media3 (`rtsp` e `smoothstreaming`), eliminação de pacotes criptográficos pós-quânticos não utilizados do BouncyCastle, remoção de dependências legadas (`easypermissions`, `legacy-support-v4`, `commons-io`, `kmpalette-network`) e otimização de ícones/recursos gráficos.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.19/LICENSE
