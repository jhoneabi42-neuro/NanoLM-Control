# NanoLM Control 0.1 — Android

Projeto-fonte Kotlin, sem backend próprio. **Não inclui APK compilado nem modelo NanoLM integrado.** O interpretador inicial é determinístico e restrito.

## Funcionalidades
- Entrada de texto e reconhecimento de fala **somente se o Android oferecer reconhecimento no dispositivo (API 31+)**; sem fallback para nuvem.
- Confirmação explícita antes de cada execução.
- Abrir apps em lista permitida (YouTube, WhatsApp, Chrome, Configurações), pesquisar via navegador, voltar, início, escrever em campo editável em foco (não senha).
- Registro local com data, tipo de comando e resultado da solicitação, sem conteúdo do comando.
- Serviço de acessibilidade opt-in para voltar, início e escrita.

## Construir
1. Abra esta pasta no Android Studio com Android SDK 35 e JDK 17.
2. Sincronize Gradle (Android Gradle Plugin 8.7.3, Kotlin 2.0.21).
3. Execute no dispositivo Android API 26+; voz local requer API 31+ e serviço on-device instalado.
4. Ative manualmente o serviço de acessibilidade apenas se quiser navegação e escrita.
5. Digite ou fale um comando, revise a proposta e toque em **Confirmar e executar**.

## Limitações de segurança e funcionamento
- Não é um agente autônomo; não escuta continuamente, não usa palavra de ativação e não encadeia ações.
- O `SpeechRecognizer` depende de um serviço local fornecido pelo sistema; pode não estar disponível. O app recusa fallback para reconhecimento remoto.
- Pesquisa abre uma URL do Google: **a consulta é enviada ao serviço de busca ao abrir a página**. Portanto, a operação de pesquisa não é offline nem privada em relação ao buscador.
- `getLaunchIntentForPackage` pode falhar se o app não estiver instalado ou por restrições de visibilidade de pacotes do Android. Não há descoberta geral de apps.
- O retorno `true` das ações significa que a solicitação foi aceita pela API, **não que o estado final foi verificado**.
- Escrever substitui o texto do campo em foco. Não envia mensagens. Não acessa campos de senha.
- O log local **não é uma prova criptográfica de integridade** e pode ser alterado por quem controla o dispositivo.
- Acessibilidade deve ser habilitada pelo usuário; observe as políticas da Google Play antes de distribuir.
- Não foi executada compilação neste ambiente; valide build e comportamento em aparelho real.

## Próximas integrações
Substituir `CommandParser.parse` por um adaptador NanoLM que devolva apenas comandos tipados; manter autorização e execução fora do modelo. Criar testes automatizados e verificação de estado da UI antes de alegar sucesso.

## APK pelo GitHub Actions
Coloque o conteúdo desta pasta na raiz de um repositório, incluindo
`.github/workflows/android.yml`. Com Actions habilitado, um push inicia a compilação.
Na aba Actions, abra “Gerar APK de teste” e baixe o artefato
“NanoLM-Control-debug” de uma execução concluída com sucesso. Extraia app-debug.apk.
A assinatura é de depuração e pode mudar entre execuções. Localmente, use
Gradle 8.9 com JDK 17 e execute `gradle :app:assembleDebug`.
