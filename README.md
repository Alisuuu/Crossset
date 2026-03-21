# 🛠️ CrossSset
<p align="center">
<p align="center">
  <img src="https://github.com/user-attachments/assets/558cac8d-5670-40ee-85d3-4e48661cfd82" width="45%" />
  <img src="https://github.com/user-attachments/assets/0a94299a-d8d4-4299-a9fc-1d3317a2f9d5" width="45%" />
</p>
**CrossSset** é a alternativa moderna, poderosa e elegante ao famoso *SetEdit*. Escrito do zero em **Kotlin**, ele é um explorador e gerenciador avançado de configurações do Android (System, Secure e Global), focado em performance extrema e integração nativa com o **Shizuku**.

> 💡 **Por que o CrossSset?** Diferente do SetEdit original, o CrossSset utiliza arquitetura assíncrona moderna (Coroutines), não trava com listas gigantes e oferece o exclusivo recurso **Watchdog** para travar valores em tempo real.

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)
![Kotlin](https://img.shields.io/badge/language-Kotlin-purple.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Shizuku](https://img.shields.io/badge/powered%20by-Shizuku-brightgreen.svg)

---

## 🔥 Evolução em relação ao SetEdit

| Recurso | SetEdit (Original) | CrossSset (Moderno) |
| :--- | :---: | :---: |
| **Linguagem** | Java (Legacy) | **Kotlin (Moderno)** |
| **Interface** | Android 4.0 Style | **Material 3 / Amoled** |
| **Performance** | Lento em listas longas | **Lazy Loading (Instantâneo)** |
| **Permissões** | ADB Manual (cada tabela) | **Shizuku (Um toque)** |
| **Proteção** | Nenhuma | **Watchdog (Bloqueio Real-time)** |
| **Busca** | Básica | **Instantânea com Cache** |
| **Estabilidade** | Sofre com ANRs | **Zero ANR (Multi-threaded)** |

---

## ✨ Recursos Principais

### 🚀 Performance Nível Profissional
- **Arquitetura Assíncrona:** Uso intensivo de Kotlin Coroutines e Flows. Nenhuma operação pesada ocorre na thread de interface.
- **Carregamento sob Demanda (Lazy Loading):** Gerencie milhares de chaves sem lag ou travamentos.
- **Cache Inteligente:** Resultados de busca instantâneos utilizando cache em memória.

### 🛡️ Watchdog (O Guardião)
- O recurso que faltava no SetEdit: monitore configurações específicas e, se o sistema ou outro app tentar alterá-las, o Watchdog as força de volta ao seu valor customizado imediatamente.

### 📦 Gerenciamento Completo
- **Abas Inteligentes:** Organização intuitiva entre as tabelas `System`, `Secure` e `Global`.
- **Histórico & Undo:** Registro de todas as alterações com opção de reversão rápida (Undo).
- **Backup & Restore:** Exporte suas otimizações em arquivos ZIP e restaure em qualquer dispositivo.

### 🎨 Design Premium
- **Amoled Deep Black:** Totalmente otimizado para telas OLED, economizando bateria e com visual sofisticado.
- **Material 3:** Componentes modernos, animações fluidas e indicadores de risco por cores.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** [Kotlin](https://kotlinlang.org/)
- **Concorrência:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **UI:** ViewPager2, TabLayout, SwipeRefreshLayout, Material Components
- **Privilégios:** [Shizuku API](https://shizuku.rikka.app/) (Acesso transparente ao shell `settings`)
- **Localização:** Suporte para Português (BR), Inglês e Espanhol.

---

## 🚀 Como Usar

1. **Instale o Shizuku:** O motor que permite modificar o sistema sem Root. [Download aqui](https://shizuku.rikka.app/download/).
2. **Autorize o CrossSset:** Abra o app e conceda a permissão solicitada através do diálogo do Shizuku.
3. **Explore e Otimize:** Busque por chaves como `animator_duration_scale` e sinta a diferença na fluidez do sistema.
4. **Proteja suas chaves:** Ative o **Watchdog** no diálogo de edição para garantir que o sistema não reverta suas alterações.

---


## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.
---
