## Muzix v1.0.47

### Reforma completa da aba Pesquisa e limpeza de logs

- **Pesquisa 3× a 4× mais rápida e organizada:**
  - Busca inicial na aba **Tudo** reduzida de 21 para 5 requisições em paralelo, com carregamento sob demanda ao trocar para **Playlists em destaque** ou **Podcasts**.
  - Resultados da aba **Tudo** organizados em seções claras (**Artistas**, **Músicas**, **Álbuns**, **Playlists** e **Vídeos**) com botão **Ver tudo** em cada bloco.
  - Paginação infinita ao rolar nas abas filtradas (**Músicas**, **Vídeos**, **Álbuns**, **Artistas**, **Playlists**, **Playlists em destaque** e **Podcasts**).
- **Filtros, histórico e sugestões mais inteligentes:**
  - Trocar de filtro (chip) agora carrega a categoria automaticamente se ela ainda não tiver sido buscada para o termo atual, sem exibir listas vazias nem resultados antigos de outra busca.
  - Debounce (250 ms) e cancelamento automático de requisições em andamento ao digitar rápido ou enviar uma nova pesquisa.
  - Scraper e parser endurecidos: previne falha na continuação de busca e para de descartar artistas, playlists, podcasts e sugestões por campos opcionais de menu ou álbum.
  - Histórico de pesquisa instantâneo (sem atraso ao limpar), exclusão individual de item (`×`), proteção contra corte de texto longo com reticências e botão **Voltar** retornando para a tela inicial de pesquisa.
  - Tratamento de erro real com botão **Tentar novamente** na pesquisa (respeitando o filtro ativo) e na grade de **Explorar / Gêneros** offline.
- **Limpeza de logs:**
  - Eliminados falsos erros `E/Library` de cancelamento de coroutine, removido dump gigante de busca de podcasts e removido spam `getSongEntityFlow` dos logs.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
