## Muzix v1.0.31

### Modo Preto Puro (OLED / Estilo YouTube Music)

Adicionado o modo **Preto puro (OLED)** opcional e desativado por padrão nas configurações, projetado com contraste limpo e superfícies neutras inspiradas no **YouTube Music**:

- **Configurações → Interface → Preto puro (OLED):**
  - Switch dedicado logo abaixo da seleção de **Tema** (desligado por padrão para preservar o tema escuro normal).
  - Funciona instantaneamente em **Escuro** e **Padrão do sistema**, respeitando todas as cores de destaque (**Padrão**, **Papel de parede / Material You** e **Cor personalizada**) sem reiniciar o app.
- **Preto Absoluto (`#000000`) Onde Faz Diferença:**
  - Fundo principal de telas (**Início**, **Buscar**, **Biblioteca**, **Mix para você**, **Análises**, **Configurações** e **Notificações**), barra de sistema e janela Android (sem flash cinza em transições).
  - **Álbuns, Playlists, Playlists Locais, Artistas e Podcasts:** fundo da página, barras superiores ao rolar e scrim de transição das capas agora dissolvem diretamente para `#000000` puro.
  - **Configurações e Notificações:** brilho colorido do topo removido em Preto Puro para uma interface totalmente limpa, enquanto a **Home** mantém o degradê sutil do topo dissolvendo perfeitamente para `#000000`.
- **Hierarquia Visual Clara (Estilo YouTube Music):**
  - Cápsulas do **Miniplayer** e da **Barra inferior**, cards de **Configurações**, menus/bottom sheets e card de artista usam tons neutros escuros (`#141414` / `#1C1C1C` / `#262626`) com `surfaceTint = Transparent`, garantindo leitura e separação sem véu azulado ou acinzentado sobre o preto puro.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
