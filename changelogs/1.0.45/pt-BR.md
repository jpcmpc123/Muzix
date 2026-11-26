## Muzix v1.0.45

### Sugestões e Playlists Relacionadas em Playlists Grandes + Otimização de Desempenho

- **Sugestões e Playlists Relacionadas em playlists grandes (>100 músicas):**
  - Corrigida a extração do token de continuação de seção (`sectionListRenderer`), evitando confundir a paginação de faixas (`>100` músicas) com a busca pelas prateleiras de **Sugestões** e **Playlists relacionadas**.
  - Eliminadas requisições HTTP duplicadas ao abrir playlists online e playlists locais sincronizadas com o YouTube Music.
- **Banco de dados e paginação mais rápidos para playlists extensas (300–5.000+ músicas):**
  - Persistência de faixas em lote transacional no SQLite (`insertSongs`) e consultas divididas em lotes seguros de 500 IDs (sem erro acima de 999 faixas).
  - Cache incremental de status local no ViewModel e paginação híbrida (warmup leve + prefetch antecipado sob demanda durante a rolagem).
- **Rolagem mais leve e fluida:**
  - Otimização das linhas de faixa (`SongFullWidthItems`), adição de `contentType` nas listas, remoção de animações e marquees permanentes fora da faixa em reprodução e redução de logs em disco durante parsing e rolagem.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
