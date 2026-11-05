# Aplicativos incluídos nas builds Quark

A câmera legada `com.motorola.camera` (módulo mcamera do fgl27) permanece instalada sem alterações. A câmera experimental é outro pacote: `com.motorola.camera2`, 7.3.41.50-jade32, versionCode 470304150. O APK é a variante feature-test/signed-fixed validada, não os protótipos descartados nem uma versão com 4K/HDR habilitados.

KernelSU Manager: me.weishu.kernelsu, v3.3.1+36, versionCode 32716, do release backslashxx validado. Incluído automaticamente apenas em A11+KSU: o kernel A12+ ainda não contém KernelSU. Instalar o Manager não adiciona suporte root ao kernel.

QuarkBundledApps é um pequeno app de sistema assinado com a chave platform da build, sem ícone de launcher e sem acesso à internet. Usa JobScheduler após boot/desbloqueio do usuário principal e PackageInstaller para instalar os APKs preservados como apps de usuário, em /data/app, necessário à descoberta do Manager pelo kernel atual. Assim o Android também extrai as bibliotecas ARM da câmera normalmente. Não altera as assinaturas dos APKs nem concede root ou permissões perigosas automaticamente.

A tarefa começa quando o agendador permitir, não imediatamente ao ligar. Os APKs são locais. Hash SHA256 e tamanho são conferidos antes de concluir cada instalação; o PackageInstaller também valida a assinatura e compatibilidade. Versão igual/mais nova existente é preservada. Instalação bem-sucedida ou versão existente adequada é registrada por pacote; após isso, desinstalar manualmente não causa reinstalação em cada boot. Limpar os dados de Quark App Setup ou formatar /data reinicia o estado. Falhas têm no máximo três tentativas, uma por boot, sem apagar aplicativos existentes, forçar downgrade ou abrir confirmação de instalação escondida. Para atualizar os APKs incluídos futuramente, revisar hashes, versões e a política de atualização; esta integração não é um atualizador automático pela internet.

A câmera é guardada em gzip dividido em três arquivos apenas para acomodar o armazenamento no GitHub. A reconstrução reproduz byte a byte o APK assinado, SHA256 1d52096ab9c1a92535fddba72a4695aad888eaa0eedc9c712cee03b66ccf26e6. O Manager reproduz SHA256 798254823ffcc60c136a8f6ae903bfee6ff4ccd164fffc8ba1a3b749df0f607b. O certificado de teste da câmera é preservado, sem publicar sua chave privada.

Ocupa aproximadamente 85 MB adicionais na imagem do sistema; novas instalações também usam /data e espaço para bibliotecas nativas. O build precisa de espaço de partição suficiente. Não usa scripts root de primeiro boot, escrita manual em /data/app, permissive, novas permissões SELinux ou relaxamento de neverallow.

Validação local: payloads reconstruídos e hashes conferidos; Java compilado contra API 30; manifestos XML e listas de assets validados. Ainda requer compilação da ROM e teste de instalação automática no aparelho. Verificar logcat -s QuarkBundledApps, pm path dos dois pacotes, ambas as câmeras, gravação com áudio e aprovação/revogação no Manager. Os apps já existentes no aparelho de desenvolvimento podem fazer o instalador apenas registrar que a instalação manual foi preservada; isso não prova instalação inicial em uma base limpa.

A integração está no vendor compartilhado. Ao atualizar, sincronizar vendor/motorola e compilar ROM completa; boot.img sozinho não contém os aplicativos. Nenhuma mudança ao teste DT2W é incluída.
