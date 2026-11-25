## Muzix v1.0.35

### Correção no Atualizador Direto pelo App (Download de APK com OkHttp)

Correção definitiva para falhas durante o download de atualizações diretamente pelo app:

- **Substituição do cliente HTTP de download por `OkHttpClient` dedicado:**
  - Removido o limite oculto de 15 segundos (`requestTimeout`) que existia no Ktor `CIO` e interrompia o download de APKs (~44 MB) em conexões mais lentas.
  - Agora o download usa streaming via `OkHttpClient` com `callTimeout` ilimitado, `readTimeout` de 30s, HTTP/2 + redirects nativos do GitHub Releases e encerramento imediato do pool ao terminar ou cancelar.
- **Instalação e Diagnóstico Mais Confiáveis:**
  - Abertura do instalador de APK e da tela de permissão de fontes desconhecidas usando o contexto da `AppCompatActivity` atual (com fallback para `Application Context`) para compatibilidade plena com Android 14/15/16.
  - Adicionados logs estruturados em `InAppUpdater` para diagnóstico completo em caso de erro.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
