## Muzix v1.0.36

### Miniaturas em Pausa, Contraste nas Letras e Motor de Letras Mais Seguro

Melhorias visuais e reestruturação completa do pipeline de letras com zero perda de desempenho:

- **Indicador de Miniatura "Sem Som" ao Pausar:**
  - Quando a música atual é pausada, as barrinhas ciano das listas de músicas se recolhem suavemente até a posição de repouso (sem som) e suspendem a animação em segundo plano para economizar bateria e GPU em todas as telas (Artista, Álbum, Playlists, Busca e Fila).
- **Contraste Adaptativo Garantido nas Letras:**
  - Capas claras, pastéis ou vibrantes (como *Flor De Lis (Ao Vivo)*) agora têm o brilho do fundo automaticamente limitado no modo tela cheia e no card de letras para manter a leitura nítida, preservando a tonalidade original da capa e aumentando a legibilidade das linhas inativas.
- **Remoção de Créditos Não Cantados:**
  - Novo filtro para créditos em kanji/CJK (como nomes de compositores/letristas como `三浦徳子` / `Noriko Miura`, `作詞`, `作曲`, `編曲`), tags TTML de crédito e extração automática de compositores/letristas a partir dos metadados do YouTube Music.
- **Base de Letras Segura e Sincronização Precisa:**
  - Conexão da aba oficial de letras do **YouTube Music** (Musixmatch / LyricFind via `videoId`) como juiz textual e fallback oficial limpo.
  - Priorização das fontes catalogadas (`BetterLyrics` → `LRCLIB` → `Spotify` → `SimpMusic` verificado → `YouTube Music Oficial`) com tolerância rígida de duração (`5–6s`), ordenação pelo menor desvio de tempo e bloqueio de mistura entre gravações `(Ao Vivo)`, `Remix`, `Acústico` ou `Slowed` e áudio de estúdio.
  - Revalidação automática e transparente de letras salvas anteriormente para limpar créditos e sincronias incorretas do cache local.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
