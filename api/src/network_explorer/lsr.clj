(ns network-explorer.lsr
  (:require [clj-ob.ob :as ob]
            [clojure.data.json :as json]
            [datomic.client.api :as d]
            [hato.client :as http]))

(def LSR-START 1546990481000)

(def LSR-ADDRESS "0x86cd9cd0992f04231751e3761de45cecea5d1801")
(def GNOSIS-SAFE-ADDRESSES
  ["0x9E7a1400Efd39176A32Ee6E4506797dACD730BB0"
   "0x77aA1a0F716E98BE1Fc7968554B2C65586CB0d89"
   "0x7dFE2213e2bFa1FDd03E8F39b8b69B70Bc23Ea1e"
   "0xDb0BB1455C0CEcD15c42274c03CF025CaE8710D2"
   "0x44c98AA6C90F530B7742B33a45d5273621E4A354"
   "0xB26Ad38491A45C44B69B1322155B5dFbFfC98CB0"
   "0x13daDBeD45d857cfFDAA0E14769aC258Fb672250"
   "0x8d170743d09b148d4fE2f13d18243b8DA4f28DFA"
   "0x15A73aD5fA95c3A81B8Bb9AE1e75797afA0749c1"
   "0xAC32a4C1719E2CCe840C13933685220C287Dde91"
   "0x16AfEb33f384d99aC27390ee6e19C30189312E2F"
   "0x9F57C77b1095BD5Db0558b9Cb9b8e6Fc67375E3C"
   "0xF2dC50beb378f48146401603B6B770Deb359F6ED"
   "0xE105C07bddfdCC5C02b14A02773321aeC416117c"
   "0x4c54158862EFca245De42af011b3306AfEb9C6Bb"
   "0x9cA43d919569D1E86799220FA9AFab71423B1697"
   "0xfB6Df432F02D4099e0c176902351D8E79120aF7D"
   "0x4Aa9cE2404AAB305a0cAe4EBfb7445894a2F2939"
   "0xf9092d4Ae81464c0A0430aCbe1D570d04AC799AC"
   "0xbCE9e7d8bdFB8c6B771b3D434b629bDF236830a7"
   "0x8099b697663447Ed94ec028E005327765A44Ba70"
   "0x20f7bC4d4C65D4C3fA2F72b4820c72C3190Ac4e8"
   "0xFAB265867B9DAd9B60483214b83A36e7ee0fe9B1"
   "0x89Bd1bE2fe3cfd496A0bf13ab78F63F82F4AfB56"
   "0xE15161FF8bD124d1D38D7d0C36ce8A5998eb25bb"
   "0xbA4B373B3bB5f54C9bec55dc245ec26C8025Ab0d"
   "0x7275d23677d7B300F955Fc4c23B9045f5C515B7C"
   "0x87f6FccE1d14515b6Cb9158605a2Ac78dA545D6F"
   "0x9aFc8f2c435cE23FcE241Afb80D8fFE0dd5ea015"
   "0x7031a99045De0392E1289E35C7FD637b1e8E9fde"
   "0xB55818c05F382f3922B26758eD6d234bC011B858"
   "0xabb5dE65BB69Ee8E1389be0a490c8e23478Ac0F4"
   "0x09239E16302cC0A6676Ba0928ac44ff922a31992"
   "0xc9B90bcf2c45b48dEFCAdecD638C356aE949717F"
   "0x53e8272301d4Db1a57d3604f8b0Cb1503D4ceFA1"
   "0x6d152c1191ef723876FF5Fcee6C9b6acB861A73B"
   "0x2D83946B530b99e823E9C9896e0e0F05BD5D1fCE"
   "0x8FF7Bf90a90e5da515B4e9eA086016AF7e8465C1"
   "0xb4e5db6ae290a8990B1316145E84DB9F55993748"
   "0x1BAc1284c4DE3dd00304369134ffF4f5Abeaaf5a"
   "0x8F24659356998bA459fEA898f738362237Eed53C"
   "0x82cA887f3Ce5F07DddB3F05c67B881Dea9de7DA9"
   "0x3e7bd7229665D19fe319b76605EDc0B437655D7b"
   "0x976C3D006B1e3cD7ef1A4FBCa9Aa34b25FF07f2e"
   "0x6B925675A51C9Ff22AFae5F3C2E0BBdC9d16097C"
   "0xbe2AEc3d9F951767034e8bcE739b227F4878c3c4"
   "0x16DD55Be903E0eD4C6fBdBa703c3C223b76cc8cd"
   "0xf29C1553a5d815148EA6B6Dfe29abfE181c019B6"
   "0xC135363F9B418740CdfD10C8B38C7aC91F215D17"
   "0x89e1d7759ff1c8f0283df0c17F0618CAF7B694f2"
   "0xC1F19a941bbAa3AA7b70e13Eed958b6744c3BAf6"
   "0x68b9AFc779d86099fd378fC31c037e19C6CC8440"
   "0xAef10838D3B01355e2531D992F700CFb8467ea96"
   "0x52D48f2A8B8B23533A6d407d38b8A19b12CA6dF3"
   "0x31422ACef5B42363F0E3b2f9090A21367B7663dB"
   "0x8A2d1c9c8946B8EFEBd1994688EDDd3F9822C747"
   "0xA58Bf3fe3cAEdAA9738FAC00857dfd9150969075"
   "0x1ddef81e9d3A5aA4D30Ff740b5c9BFa66A176969"
   "0xc1fB49056ae14BFd814630a21cc1E902D229797B"
   "0x7A0C7B161f706f3d0b806e33606f623E14f94197"
   "0xD15C6c2848C4E132c925d55F3d6bf41799DDbE54"
   "0x0ca7F4fa293118f7F31cD8154d5242323c9e572e"
   "0xab6ac840A1852046c8953333FC4f869cBb64B794"
   "0x0A194d74Ee18ffe5B464E74f2Ff745BF9b7790FC"
   "0xb73532B04FB598F5d719Ec40be68DB02F798bCf3"
   "0x6F9a33c8AA249bFE63b285f3815d08410675513c"
   "0x307191F294323F6d84168ca88d9c8e69EAd638f6"
   "0x30C011F8B14aDd6c009b210AAD2E084CD152f2d7"
   "0x3e26190DaaCc364ECA378e4591bca6EB6c05078D"
   "0xb5824001CEF4324CB211606BB10Dd69a6fc3591e"
   "0xAf1f5c6F8D8065ecD8106212b0241cc2345Cd7db"
   "0x598874Bfb8865f6D37bbdF56346DaD780973Cd6d"
   "0x57a50a09bD0378B0154f27d9d0AC87Ae9e8c9ECc"
   "0xF37e79d7e1BB76875D288D2719AFafA640479812"
   "0x157C68B10a3F9311Ac6d01862b69203ffe5e08d8"
   "0x6229842F1C8243ECF90Dc271eC8D069E65b43e8A"
   "0x6eBcf2f709693968710e441c9F70573Dba718627"
   "0xd32469f7aFEfE0C9E9592b460e5D310980144bE7"
   "0x860D04E3cf4856f1697E09A9B64FAB4B4Dfc5d0B"
   "0x09a8Ee7B662cC0c8D1eFFaF6C9FC526383F5BCA1"
   "0x17DcF09aE8525D49ad92a319E603cE564C4ca002"
   "0xa6E9822Bd49Dd0FaeC0664870beeA8D48a122051"
   "0x5eB03d359E6815d6407771Ab69e80af5644104B9"
   "0x067A373420E67e4C0eDbB547eD6133b83Eb8C7a4"
   "0xDb37c2d3825b79203bBAa71166FfDea25347B673"
   "0x53754c51Bd2DDCCDbb85C8E63827011d2fed646B"
   "0xA3C1Bd39Ba24420cd04d6F97b78b911698a5A67d"
   "0xafa96b1aF942Cd9AE789c5e4D86E4c7174503eFa"
   "0x2BfA25F946FbeF42333098E8898171817d1c3348"
   "0x8ba8a3e0Ac02fe8F91F30afcD9f19720B5Ad04a4"
   "0x00b2E3c4347E314A931bfff899d239222eC8Cc9f"
   "0x4cdcbAed9bE1BE2E59E349e615B4a7Dafb934A28"
   "0xb17C5CD5671233Bc1697b50E8298d401110b90Bb"
   "0xaaFbF2B1040dB63d8e85197303C3Dc73977e2EfD"
   "0xA029107e4e564fcD858489874e122117ce40C135"
   "0xac4D0d17aEAD3aE5555D6bF5b22F5f9470E67Fce"
   "0x149B0C5E1e91911C141c24db4386589B4E08ba09"
   "0x71D18Fd8f78C4afDa917A6297F611f012b05d1c6"
   "0x0cdb9333EFE7E358f1bdFAf9980A41846688DB8C"
   "0x0C6df9Ca3805B95094596cD6fb24d335B7adcA0E"
   "0x8fd100c68015993468bd41C7410e46044111131f"
   "0xeCe1Ed6fa335350A242ADDc97493b781Ce17F960"
   "0x02C1bE7CE8C07096308Db18b82fe446c12dCAC5d"
   "0x7dd06D8144238E53f01D2268B6760ec252B83520"])

;; these lockups were just a prank
(def JOKE
  ["0x616c480b81123bdb7d752474e3b4a6be09ca8d2a"
   "0xfd2b417acdd07aea5b28671e342209990d5c29fe"
   "0x0b1d8c9f512f6ddd4cb42e0a6e38615ccbc6c7af"
   "0xe4838ef1ebca3e9d616d227348a3d5948af4ba1c"
   "0x14553486eaa3f2623ff0356f420b58c407758294"
   "0x31262760daaabba92098f5e8c09203ea5453d127"
   "0x57c8b9a2156113752776012245027d15f0beb734"
   "0x44becb646d162837d6cd8e0779150c12e7f0b066"
   "0xf7f26d225c09a099723cc4ac53f2143208accdc8"
   "0xbf7cbb8a68fa766ca81b6aafb1ea2e3f423c24e6"
   "0x7d82b9f17468ea29a9d4148b950a77a089d2b732"
   "0xaa111a5185fe2800391bd2e65b334bdf001e7487"
   "0xe940eaff5fde449ba520785f4a7db4ae7dc213fc"
   "0xe04487276cd173d8b5118a7f778402bbc3fbf015"
   "0xf75802242c362e73839f718dd3fcbb4b0026694b"
   "0x5c8344f73d2f234706ec7df39b9216f4b5887043"
   "0xffe2a360360dc90649c84e3d813612ede5d2f0bf"
   "0x841c0e6d1d0488a6de8001da48f90cdf58af3851"
   "0xb8981f7a2c1a7b9f9ba6d9806bf2394000c2b92f"
   "0x1515625b65120176710ed31f53d5f5b4c630f06b"
   "0xba63f15bb7790e34de727b42c3f909117efe72d1"
   "0x9d32500b6033606edab2f3c3c161cba962a97e04"
   "0xbe4fbe499c8bba2c061261e7fb8e25f4002c617f"
   "0x6da184a1a1b8ebb7c27439efefd94635a89c49b3"
   "0xdf70d3ce504afb69b31b07db1162be64fa677114"
   "0x5b86494388f975233ce5c3b5cd96921a3be6a7e8"
   "0x066c53a422604d47f89bc197dc96fb4d14d91ace"
   "0xf0be5c3a14ed8b9b41c46aa4a1fba132b5022415"
   "0x5047e5e0ec328b29a9a6b0493a98a0e45adc0a56"
   "0x1bec0e167d2b2bfa50fd73be0f54bfc943d8c775"
   "0x4f31dd68c1f0bd4f2980cbe222d4cc712183f661"
   "0x2acfb62125e3c7a7671570a9e452aff2700cac47"
   "0xd31ba9cc5611c5bac6e000ac9795b0745ac8c3c7"
   "0x2f0cc6ed2f987a0dc390000b4975e7917438684e"
   "0xe87f1615c61f3b9469f2fe18b18275fc153f781a"
   "0x0766120ac905c0ceda543a58e0478640bb36e870"
   "0x615d103e779fcc4125b8b6a0d86119f2b77d3c13"
   "0x3ceac88bcc8163e351149c2a611293913c7e7fda"
   "0x777825755eaefc95fe03151aa228bda0ed2cc279"
   "0x2e43fc3c8946a981ce76e1af6b7a92684307a572"
   "0x43b874ccfb9140115ed3ac1d84d6ffff80f56214"
   "0xeaa0c6ffe5468c6b44c2a469d27bec59958d1b96"
   "0x6baa273323edef4a00bbb19198998c073438cc42"
   "0x9eb99f8f4e604bd555d5734ea0ff6e9fe3b7789f"
   "0xfddf4ed7e59f1ae73a1904546280107cd8d873c3"
   "0xb6137b5232a64a41e1855bdf2a705a1fd62d642e"
   "0xcaedf5af5d421ebeff1e7a497b5af1aba1795b94"
   "0x0e49ab776681630f34d0f0c57d261fd5918f51c8"
   "0x2ad1191ddaf7b47fb55883890b5c08679e72990c"
   "0x07a50d9d58203343e0b252cbad7e1be6b54224f7"
   "0x5bab5bf43e094690749b058a68561868bfc9de61"
   "0x15b88443995602d9fe7aa0f72a90eef132df2896"
   "0x3e8c0cbd2a59d2d4b7d8396acc04ab349a169286"
   "0x1cf8d5110ed0cc6d7a8096864ec4215a7590dee9"
   "0x9d92dba13943c1ff36c527618233ef5ad49a14ae"
   "0xde7aee784a4ff057d97b16b0649bb57caa42997e"
   "0xf19f0281e831e5820190baf0992276c5219b4d5e"
   "0x5b7390b1605a67281a6dbd156dd01aeda925d7a8"
   "0xb57b29ea39ea4636cd5513f6bf04eacd3c951604"
   "0xd5e69e7c2eaf78a544b8d1b5ce261a0a698ac3e7"
   "0x6e4eeabc70534f0a97a27663365c404a89a29ea4"
   "0x48353a9af7f835aa7ea878fe56984f20eed8230c"
   "0x76636525e1e8fb47737c831a705bfdff16444754"
   "0xdaca41635b747585a761c48489492f830ba53466"
   "0x42a6b62cf278c814b193213bc815af103b890fb3"
   "0xc0bffc263d80c509ebf07a74f9b8bb70a3ddb242"
   "0x9427d10c956f986ed9ad20a4eba831fd8b249c7c"
   "0x1567415f7c9364239e70e5197bb9d0125211e859"
   "0x30ea4f469d5d374a36c369f6b872aca374d0c1f5"
   "0x1c7759809dac18f5640d76985aa4dd311cf56c8c"
   "0x9488f19152dc9da0b93d925bd6729201f94171d6"
   "0x2bd9bc6e1eb2a79639a3786408076a1712a26b47"
   "0xb1ba6b9a63143981e23d9e6ea39d0d903764d6e4"
   "0x248226b1972c13579693ad03b04303e68e7bd489"
   "0x71c81aefda50edd46a81e2a2c5164f27499aa7b8"
   "0x795c562f4293b865bae6f36f37f7497ad037f868"
   "0x80f3a01a432d69cdf579c27d7558463673ec03a4"
   "0xe277847d732e3579347c66baac81602b19669463"
   "0x4fed42d367f4ca5cf0d0356ec03477d31df35cd5"
   "0xc60a6ff6d50edaf19a2b27c0fb6974bcc2f989e6"
   "0xedee82776c512917139befca2cb65af8d33ce868"
   "0x220c751d752cc2a17c91ab123b6f43a5d8be873d"
   "0x5099ca9ecbe4d56984f333e2a0a73207fdf28e27"
   "0x40f18a81011caa903bed302162a08d0545eac7b1"
   "0x895ed77f58e249e0edd101d0600cc410a8abf9de"
   "0x02dc54ab6e220ba634e5a44fdc6e224de2bd55d6"
   "0x1af33b81cef2df1363f62c7136642d2cdee9b39b"
   "0x9559f3877b4ea265751121273416f565357dc0fb"
   "0xb92a39d234276244291676783eecb1a494915270"
   "0x1d45d3673143bd5017e688a9f51dfc6396dbc81d"
   "0xab08471574ff734c5875129b3ac40be3aea8149f"
   "0x16bf979c9122595e66fb2d8471e4713437432f5b"
   "0x242fab4740e5f2e6462c1e37dd80e2359afff6e5"
   "0xf2f74bba93f25f6eaf9afd5287747cc19c350c69"
   "0x6c4c7331e6a806a5b4114202042f82c8a260ec57"
   "0xce0856e300684b4aee9f94111fc37c245c0b6713"
   "0x6bd0d475498ae88f4342d926f046e353c7cfea21"
   "0x594dada26d7945420f3fa8a18f664b4e8c0a4138"
   "0x3106f41b69d66d54ab1fd2751eb962d553f4a4cd"
   "0xccb28beae6bd3e4094fe6bcfbe271e511999e917"
   "0x4c162e91a6c96c208b52c415a1cd9db5b20622a0"
   "0xf36fb509e35e2cb4d713cebe4ac75a54a19dcac8"
   "0xecd3f1c51de697a03163c0e81bfdb59d35f26a38"
   "0xf7e2a87fae52f78987704ee4ff5d126ae6720cf0"
   "0xabdd805029c8f3c40138bdbe75e0d6f8a14f6d03"
   "0x271c8e7f09d8911acae8cf5034c3d6433a00c6c2"
   "0xb8ff1083b1042c23fef72687cc518d2384378ce0"
   "0xf128d5062d9b079aaab3e3c662d964ddbaeaa374"
   "0x4efd8a253c1a3a7d502c4266fa5aeacc00478b9d"
   "0x41d9da60934669b7e38c5cb9fa1f4421d452d29d"
   "0x2354e4e244456bf4b3c120d5756fa2cf6f911ee1"
   "0xbc712b39c093db7e16be71ed0dbe5e45b7a8f3ea"
   "0x0f6b0d07b77103450c363514a18548db204d5bd5"
   "0x4b7b2f74cdcf725a955231bcd32b27f8162e5ad4"
   "0x49327109fbfe1716b47b4d8652b5824d09367136"
   "0xcdf8126816954740ddf08ce44410d825e6cf82e2"
   "0x5cc911152eb2a3d6baba7e49857bf7d22e7132f4"
   "0x552c91a596fd67a29cb51d6ecb31bd3d41a287af"
   "0xc9eb67adbd429e4465c6495a85d60bb21696ea1f"
   "0x546fda89a02f95d610afce877eec5df9fff2d10f"
   "0x8d495d62d2254856c7d9f4f926baf134a040d804"
   "0x71134cfce518c0055076946b7c357404f4d2b59a"
   "0x9c01600956c612023b7ca000d252c3bca5068142"
   "0x62cceae99a4ac8cca197463901e9c6f58e2d94dc"
   "0x2e94e8965b757e4235206aef8f9661cf6292ed4b"
   "0x014ee0483b4a51c4ea7456b0af8336af28363978"
   "0x53bba118e3cfe27593611f607e561f6fd69d4802"
   "0xd1bc34fefb3d91d99a8999cb5b90318a7c7f74df"
   "0x3172bceec3707045da310e9074e01cf4946a5cc8"
   "0x2a13a29d358f8a2a3024145c79194e0ae93e0fd6"
   "0x28b051575b674d8bbf954beb86a08e5d386dcf87"
   "0xc5380fcc87ec3040858709c115464f4ab790b185"
   "0x86160feab0f584bef8a28c9267128268e2c2fb81"
   "0xd2ea711977f34ced520b12c42036491e34b11d70"
   "0xfe1dafc10a453ed4c8c9c902f0a736f32c2fd7c0"
   "0x3b24904137a23255e016195a3b5601dfa68c83e8"
   "0xaec18db577556ed6cb413e43e5150db06cb5266e"
   "0x0e7bec636e535ab530323bfe4306b4ca41272b50"
   "0xf31f4897fb518b7b0c5c7c8b682cb96ed8b1301a"
   "0x4feabe9b0e02565e1b7002cb6ed2d74826fa1f7e"
   "0xb3f2f6783c085358a960f327ea5d448750c69247"
   "0x92008d95d72e275e51e3a927553d4f13ea17e92f"
   "0x69d034817e04fd97ea949dddeeff9ae3675c3aa5"
   "0xa9ecc1b2981b06b475324c390e94ab192db6d6ea"
   "0x0432c26fce0217d5f6ef988b53b37d12050e57d9"
   "0xb4f95a8c80611b057999ca4b727ecbd8ccc27837"
   "0x031ebaa9c264dd1a39c732458cb5f560268ce1f9"
   "0x2d1047575c10147c2fad0a6f8328cca10d0b8b08"
   "0xa88eb4e1fff42a557019698665f76b2d3018aad8"
   "0x13d13aa27762e77e14fd79a26bf037ec4810813b"
   "0x4fb07fb091fd1d5ebf010c5e9601ab262bb22c3b"
   "0xfcceaddcb37e124811c981e008c59ecccb3a6335"
   "0x4e9086f8bf623a96067125be2c7fe0d3bdc2ab0f"
   "0x5397b2b9939204e87be4408d48442ca0a0f227bb"
   "0xbbcb9e22fa57efd91c813c98101ffbd8402fe577"
   "0xa40762cdd508cdee100ac150c378da86b5732482"
   "0xcd94fb4d41ba1049de0cbad98af35e3792c31dd9"
   "0x3114907924b2f6f82b55e6a10a950775606a1bf0"
   "0x8b3ba6ee54829e6c55f13bef2bfe7f2a12add83a"
   "0xaffa976bc953b9d91adf1a65c9a1c72e425ce5c6"
   "0xd17f939cc0f318632f8bd71fedb01b83638c97c4"
   "0x863d70e8abd5790a4db6b72f3e4d94da9cc3aa2c"
   "0xe39645c99d7b12061a95c4d3b81807980eebb574"
   "0x52cdbaf7f02f8ef3ae9c7ebc0e4af4777aa0b11e"
   "0x7924c2083a9fc25c75650c663b626c8b3d8a7b77"
   "0x896ec45652aeb13fbcb4031f00245140d19e459f"
   "0x024fcf4061a2f2d35dd88644f7a993873f2fa7d9"
   "0xc1abce0336ee5e783d58a82c631ad111b9059069"
   "0x27716e502ab7f5573ea9aa4401438e2ecf7e9876"
   "0x4a092f7400769c10cac2547c241c2af87b31ed9d"
   "0xc6afcd40f37c1e569e4d1c7ef96c2c581027d74e"
   "0xf2194d5336dec1fd4e3ef4d601d07458bb1d94db"
   "0xe3475c65b7fc042f83dd5465fb15f7ef978cdca8"
   "0x1fa8cbd828232acd253e80f6c6df1bdfdcf935c3"
   "0x7eab73023e0c6e36ee0720e6bde89715af1bbe63"
   "0x5cd8230457751f4834d3635f880297d536993a3b"
   "0xd885333c99a73deed86ece05d0c7cdc8c295a3b7"
   "0x68db6ccdc4d1a48aad8089ffecda87d8dd6124a2"
   "0x22e94f41fc332742bd889a1aa867b420c5d9783b"
   "0x2ce5f9e9c5c7c104da7ac9301f9dc6825619861c"
   "0xce77966caf29bb0b68dc356292849942c42d1886"
   "0xd26b94c7e4542ff1fc4af191a8c40eeb161078fe"
   "0xcdde4f69e2b15fcd3f22495b9ba4e4f3b0319444"
   "0x6fadcd9ad513940e63ab15b84d857a1e0b8eee38"
   "0x2dc2bafac9fa3bd9c77bf68eca2ab61aa43e1ca8"
   "0xe93aa3287b78278d310dfb21ae6319c03e4aa44d"
   "0xcd8601a72bfa9c7cd9ab816c156854c3f14595f8"
   "0x0928ebbc8f008d4375f9de52ddc4628a61dfff79"
   "0x9ea078e501a8c8659570760d30fb6772215a34eb"
   "0x1b75f5f49835919a818b7883a94246d57e50380b"
   "0xe2e3295ca6662ce9fddcc26b0471d3ece8b83b20"
   "0x58df498c37db1d9c6ab9c027ceb389bff881ab41"
   "0x3aed821ec028e42f637eb2fd90d7f0d4c8b652a5"
   "0x84e5b005a9017e7baf8719cc57260f99db5665b3"
   "0x61244dfbc625a058b6d79d151dbe44c7d42e992a"
   "0x941dcd40700bdf43b41d32b4516134f151feffd6"
   "0x50d561403a8fc3ac086925faac02612aa031cf39"
   "0xa45e744e6a106eabf8562e2554235090d03a4196"
   "0x64514082d92cf842e8ed8ee545172912c2f2428d"
   "0xd9bdc76b25a01870c0b4188bef786eff81d477a3"
   "0xb356f44693d6988ec6aa7a53c88d347278466454"
   "0x7d73a3830b3df009bed1cc7f146ef0eb0d368539"
   "0x09c470640128658f1cc4e94d0abcbaaaec047cb6"
   "0xa8fb9c8ce7e48b4915d4db4998207503a9719998"
   "0x0c1c2422f620e14d05f9744af708cfa8a968a7e6"
   "0xc494ea55740bd2fe68a4dcac23df59be19bbb238"
   "0xaaf38697dacfa0b12caf376a698f8c02baa4be95"
   "0x0d030a23d0246e3f577e7ae522f8e9c33f8c9510"
   "0x6f980b77ffb57ab70c293ae52b1e2b247cb18743"
   "0x04b7cea976a0daa432cc56bc4a3828d72868263f"
   "0xc0cb0de42112ec27e60b2ee0f321fea829bf3951"
   "0x561bafe35c1cf43abaea8595ad02159c56bf2ec7"
   "0x279de5b86ff9c6163f5f28b023a96ef04bd4aeaf"
   "0x670e4e05bfbb717c1766fe37d02199c885a2c0e1"
   "0x11206081cf06121b5b4a5396dbae9a4b92c90422"
   "0x07bd8a30cba68182c74dfffb2be56c7d61aadfc1"
   "0x1a9f46761b91d205fe25d70e5d97d8da4d88f141"
   "0x5ee5050610b4b582a677a5e41f1bfd1ef07bbc4f"
   "0x38af4d61eb8df004ee961623876859d4e603a9ea"
   "0x09fd14a7036dc94b1031e5806ab716080c36acab"
   "0x76b0f35c3a37801f6cd646c09eb7b5ecd282fd4b"
   "0x97ab592f2f746420f474e1a065cdc4ee3516296e"
   "0x35c781149500e23bf673ee6650c451dd3a0e818e"
   "0xc44612d1c30b31bb28967b1dfa5a2fcaa4be67bd"
   "0xef4d3cf6e94b56d2961e6d6bcaaa27802049f02d"
   "0xf8ac868927cb5bc6a5b439264b127c6f3f431803"
   "0x0820f2889ee8124e2ccf6e95aae788ca11258255"
   "0xc6df67d039a861e051b31d371fc5d4802f3445b7"
   "0xed9b06f80f68b45e543860ae2322707236fcbcc9"
   "0xb9955d71e55d44d0978e933f0b320527fe86334c"
   "0x37c371bfa03933729a238904c26a0ff1c5279fab"
   "0x67d3b8a7a6a4f41e9b01cc1311ac1b99e16802c4"
   "0x61f9dd1c1b87143577da0d6f4acee5c58fcc1972"
   "0x83bacf6677123d3312dece2db192ff74f4f92d0f"
   "0x78acbd584d874ea8e1e1ac651f5ba9e1f59e264f"
   "0x5f4514649eeb85e398b89ece870299a395624b35"
   "0xe1b32304eb10722256dcd5e0e79b48eb1a78d5cb"
   "0x6f716f804729e9df0017a5d9f90e8d73d0dae197"
   "0x52f5efc73160db7a07ce0d821f8b30d052596b47"
   "0xc26a6216395a73a08098e558dad069e149a45953"
   "0x9295eaf1ee50bcf4437029948a645185108d7974"
   "0x0b6161231a52d4b2844c66deecec25c62c920cc2"
   "0x0b389310609a925a0ae25ade859fb0dc49122d24"
   "0x588a8a5e00e5bb23de6c6033be76dbb5ee1d5c8d"
   "0xf3b1ed07cc9a4e3d4ed644fe01db092dc91ef931"
   "0x1509a83b8c1acddb8472744ca28d0aea22e6fa06"
   "0xd0c1379b0b247aab57d90ee7eb0ea9577fbd53ec"
   "0x29aca53714e534d92762a6b88175f139ed38ac94"
   "0xdb42ceadb3d950b90309952664ebe730ff01dac4"
   "0xeb20d15bb620c673dff8b46dd4136c4f899458fd"
   "0x5210ea97d2f2f468a626ab46eb927bead90138be"
   "0x8a40770bfac307c866a92e8ef28c2b3848d07e5e"
   "0xee3b5d1c91ce250af01447353c1c03cc5ec7f418"
   "0x3094eba991a4fb1e962be2b6eec3e7370684aaaf"
   "0xdfe5a783eed8743ea0a0f09af80e45bff036f951"
   "0x0bf23d14483e0ff62d58e4de90cb69fd5dda9430"
   "0xfa870b2a1256afafcfad26117bde2677056259e2"
   "0x3ed5ab99cf376d48e7f4c17c92b45573818e4f58"
   "0xdedc08ecf22f65c047faff1ce442802f1e186c2e"
   "0x3956e807d7e39d96e4805d4995a4f7b7a8e0d2c6"
   "0xc464be9abbcc81f2ee736e4497eea3dbd300ac16"
   "0x6c65fb326e7734ba5508b5d043718288b43b9ed9"
   "0xce78072bd2ec84f4d60f9f1eaeb626636c531986"
   "0xd92f17681f1c2a0117c5d5b7fe30cda8cb026a9c"
   "0x102cfe4286a2fa712ad19744310777f08b34c58b"
   "0xc135d0ba35d68b919133b3159a56b6afb9cc1923"
   "0xab9cf911693ab8ffeef2b81e22f0d568154947f6"
   "0x4dd82f79c03a231c90e3389d5a730fbacd68cc2f"
   "0x9667df955d6262498564ff1832b3bf010a5c2018"
   "0x255effbe300a53d04b0bd98dab899d4c2c5053d2"
   "0xa12b3882f4bbd101c0ce32af0f26b61b3e400952"
   "0xccc31405a8cfc73b9ea107b49ed72b91af4c4e47"
   "0x286b555078b8ac4c3926e5ddd0d69e1e16777dda"
   "0x5a4c147ac23140f541f3c468348271d3e778465d"
   "0xbb344a07e31d9224a94b04bc5e8e80467098002b"
   "0x7966b54bc15593be66b610af32edabe65ff8e1ec"
   "0x23ac63230224f8dcdf54e388b50e5ed16e9b4dd7"
   "0x8cea69a941d6ad4c9a9c825cfaa1b2974aec4a76"
   "0x3e29e76545b2fd875df2626e3de7bd74adeb5b8d"
   "0x0d39586775e27f026fdd48441952705c92a70751"
   "0xbc4d284227e6866640b329f8f7b772579b316bd4"
   "0x6b6675a2a58186cba0014eb9bc23764863559872"
   "0xd977b146accacf8701a56a97d2c98955f28909e2"
   "0xd8ff1c2b3506923064dcd4ce3f2986e2812e04ec"
   "0x3ff54098a3201e75b446dba288db8a7fab03e2eb"
   "0x46051789d560daefba2e8b3b631d108e36477230"
   "0xac78be88cde834495429feb4b87a5d4678a986f0"
   "0xda25f2ac41ac1032ec575f8e256d6213409dcec2"
   "0x661814320dcdcc346fac688ab7d61da0c6081617"
   "0xcdcb8d374da4fe8d7c21cbf5d4e70885484cc27a"
   "0x9343fc68007d42a5414b53bb70fa0150c21115c6"
   "0x0d308f0645c487645532141d64ccfb38b03d3d94"
   "0x6c542088dd2a14a09f1aaa7f599648add90adc7b"
   "0xb2785b8db3e857e6fb48d70322ed0cbdd9465515"
   "0x0afce60e69b581ba6d86458dab6309a685bde744"
   "0x058b9a143180b83f10369ccbdc53032c0e5ae56b"
   "0x00125c609527061996b1f30fccd75334495691fe"
   "0xb52f4d17f2a70fe4492e784c52e9c7f0e9ac7402"
   "0x445e6e83e87c32024bb4774a776017b468f012ec"
   "0x342b38420bc0e3abff97c441968f22aac93d3a59"
   "0x0e54f3f3a096b0646ff329b404428728c914759f"
   "0x060a3100514630be4a3e15e16257d7136158f495"
   "0xc510fa73c4bf7e01f985acb1859d4907063eea62"
   "0x9893e05c2a7a9ba3261c58710d297054d756007b"
   "0x7f5e917e5d32dd5d7e4fda0f8356e3be506b1c79"
   "0xffe0beecddc515faeefece0354e59e15dd29f031"
   "0x669a5809bb659951015a9d4c7b9c6cab624dab3b"
   "0xad8d70535d9cd583d7b4661499c5a6011ef08956"
   "0xf5f849c1c5aa93e1231220e844a92de39fbe5f35"
   "0xde82bfb448eb6b9042003e47f1fa36ba21013c7b"
   "0xba1cd32dcb24073155f3e91a7a1a5ed257d4eb82"
   "0x4971f1d84a9b52217345e144e931f920a0541b28"
   "0x34b0607fc76a46c3d258d49523ef8c759b5acd23"
   "0x6b87de852e06057d22019f7e7447075a78529723"
   "0x569fdfc6a2f244bfa9dc646059e015cd3fc330f1"
   "0xe39049cac5e46012046f43f391f5dbda2555eb68"
   "0x20fcfc658142bf0e65c86b4cf5dcd8bcf871c24a"
   "0xc42028264b9509c9c99d6e5e0a52728fda6fcde6"
   "0xf23febd564c2aac572b0b0ed7d5588df7a7d78d9"
   "0x51c249d8d5dd9bba882c19ad74d04c5eaf680533"
   "0x816e93da554027e0a30f60c46850ab6e89f7a018"
   "0xd6ff095fcc19e56af071acbbe73e5646a8fb98d8"
   "0x8e300735ebb65d3cc2f6776f6b6810c8fd3458de"
   "0xbd396c580d868fbbe4a115dd667e756079880801"
   "0xfa482a7da38d32a9e6cfdb262166af3e9b174f84"
   "0xb8be9b76dd6d2eb31a99006b934e303d16e171b4"
   "0xe08cfc0588a2024b5225c6ecac440b9392621a1b"
   "0x7e4cfa2d51b43cd7ac657bfd25d26eeec32cfc22"
   "0xec4f9392112c343b024862d46f447e0e43f952f0"
   "0xada5b3bdf74d847c210ed0036f88ebf7a40f11f6"
   "0xbd0735e4e2576cc62f01dbf85893e924958e6ecc"
   "0xc5bd5711537540e5135a99178e35deefc0102db8"
   "0x36a078c327e465b785206d9abbe63aae1cc497f2"
   "0x56c55570eaaa46a9fc43527bb9b2ea959087bf31"
   "0x5b55059279b593f6417ff3ddcc2f34c70260c45c"
   "0x6c0c401b5381be08261f9a12d52cf9190323b30d"
   "0x93737572f931294008a52f89d84afb9d023088eb"
   "0xacd87c656ca309806ec0bb1b4f2d448fbc4e781d"
   "0x28c0a8cf6f46d6055b41092c8209d972de210c49"
   "0x408b79e5782d8398e08f08aa1bc2782dd2ae0d88"
   "0x19398dac6a5ef5bc8e96ead824044d77d491bd50"
   "0xf231a4628e3fdc5598978d01ea05b40035b0df21"
   "0x3f92571794461613c8ffab7eee6c8bfbe7a1d498"
   "0x890e67b83b540977dbf24ea26445839b4896fdc2"
   "0xcc1f72805f5675beeebec309d09de53ca12b2e1a"
   "0xf023383a80ab5462edb81c3a5209059921ff556b"
   "0xfa7127ded3b95acbf56898b4a498f05e3c007f8e"
   "0xd0e7f19ce8d9e7b6d2e6f7a8976a3c35e688dff5"
   "0x4d5f7350b60cf1eaba1aed6a3a7b50016f7df0fd"
   "0x38f004fa1169314314e60e8817fbf7b1e8a91db0"
   "0x0c3f04867d6c638bbc26ffe694a1d35265756911"
   "0x437a1a35e90157a2de4661d294bde7e0654727e0"
   "0x20d86d0e76857377a5eed678bf4ecab513c53844"
   "0x7296ec708a03aea66f475b301fdffee37e19acef"
   "0x3dc3737aa1ba23b5a14924c8f5007eb40ade9e40"
   "0xffcc2bd0008f9a8d7219ac0bf880adc8a09ad481"
   "0x84445e8c1d772f11b15a9fb5d022db094df51903"
   "0xb604ce1dee8452e01af4ad16c721006dfca4316d"
   "0xd2cec9d064770abcbcbcd1dba1f71921588675b7"
   "0xa8d9386a24463998cd9da65cf9461f0c24cd7070"])

(defn safe-parse-long [s]
  (try (parse-long s)
       (catch Exception e s)))

(defn parse-deposit [timestamp s hash transactionIndex blockNumber internalIndex]
  (let [[addr star] (map (partial apply str) (partition 64 s))]
    {:address   (str "0x" (subs addr 24))
     :star      (ob/biginteger->patp (BigInteger. star 16))
     :deposited-at timestamp
     :hash hash
     :blockNumber (safe-parse-long blockNumber)
     :internalIndex (safe-parse-long internalIndex)
     :transactionIndex (safe-parse-long transactionIndex)
     :type :deposit}))

(defn parse-withdraw [timestamp from hash transactionIndex blockNumber internalIndex]
  {:type :withdraw
   :address from
   :hash hash
   :blockNumber (safe-parse-long blockNumber)
   :internalIndex (safe-parse-long internalIndex)
   :transactionIndex (safe-parse-long transactionIndex)
   :withdrawn-at timestamp})

(defn parse-register [timestamp s hash transactionIndex blockNumber internalIndex]
  (let [[addr wnd amt rat rtu] (map (partial apply str) (partition 64 s))]
    {:address (str "0x" (subs addr 24))
     :windup (java.util.Date. (+ LSR-START (* 1000 (Long/parseLong wnd 16))))
     :raw-windup (Long/parseLong wnd 16)
     :amount (Integer/parseInt amt 16)
     :rate (Integer/parseInt rat 16)
     :rate-unit (Long/parseLong rtu 16)
     :timestamp timestamp
     :blockNumber (safe-parse-long blockNumber)
     :internalIndex (safe-parse-long internalIndex)
     :transactionIndex (safe-parse-long transactionIndex)
     :hash hash
     :type :register}))

(defn parse-approve-batch-transfer [s from hash transactionIndex blockNumber internalIndex]
  {:from-address from
   :to-address (str "0x" (subs s 24))
   :hash hash
   :blockNumber (safe-parse-long blockNumber)
   :internalIndex (safe-parse-long internalIndex)
   :transactionIndex (safe-parse-long transactionIndex)
   :type :approve-batch-transfer})

(defn parse-transfer-batch [s from hash transactionIndex blockNumber internalIndex]
  {:from-address (str "0x" (subs s 24))
   :to-address from
   :hash hash
   :blockNumber (safe-parse-long blockNumber)
   :internalIndex (safe-parse-long internalIndex)
   :transactionIndex (safe-parse-long transactionIndex)
   :type :transfer-batch})

(def m
  {"0xbfca1ead" :register
   "0xe6deefa9" :deposit
   "0xe596d811" :approve-batch-transfer
   "0xbf547894" :transfer-batch
   "0x51cff8d9" :withdraw
   "0x6a761202" :exec-transaction})

(defn parse-transaction [{:strs [input from timeStamp hash transactionIndex blockNumber internalIndex]}]
  (let [timestamp (-> timeStamp BigInteger. java.time.Instant/ofEpochSecond java.util.Date/from)]
    (case (get m (safe-subs input 0 10))
      :register (parse-register timestamp (subs input 10) hash transactionIndex blockNumber internalIndex)
      :deposit  (parse-deposit timestamp (subs input 10) hash transactionIndex blockNumber internalIndex)
      :approve-batch-transfer (parse-approve-batch-transfer (subs input 10) from hash transactionIndex blockNumber internalIndex)
      :transfer-batch (parse-transfer-batch (subs input 10) from hash transactionIndex blockNumber internalIndex)
      :withdraw (parse-withdraw timestamp from hash transactionIndex blockNumber internalIndex)
      (println (str "strange method: " (safe-subs input 0 10))))))

(defn remove-joke [m]
  (select-keys m JOKE)
   #_(apply dissoc m JOKE))


(defn fetch-lockup-owners [as ts]
  (println as)
  (let [res  (->> as
                  (mapcat (fn [a]
                            (->> (get-transactions a)
                                 (filter (fn [x]
                                           (and
                                            (= "0x6a761202" (get x "methodId"))
                                            (= "0" (get x "isError")))))
                                 (mapcat parse-gnosis-tx)
                                 (remove nil?))))
                  (map parse-transaction)
                  (remove nil?))
        more (filter (fn [e] (#{:transfer-batch :approve-batch-transfer} (:type e))) res)]
    (if (seq more)
      (recur (clojure.set/difference (set (map :to-address more)) (set as) #_(set GNOSIS-SAFE-ADDRESSES))
             (concat ts res))
      (concat ts res))))

(defn call-lockup-owners [ts]
  (fetch-lockup-owners (clojure.set/difference
                        (->> ts
                             (filter (fn [e] (= :approve-batch-transfer (:type e))))
                             (map :to-address)
                             set)
                        (set GNOSIS-SAFE-ADDRESSES)) ts))

(defn parse-transactions [ts]
  (->> ts
       (map parse-transaction)
       (remove nil?)
       call-lockup-owners
       (sort-by (juxt :blockNumber :transactionIndex :internalIndex))
       #_(reduce (fn [acc e]
                 (case (:type e)
                   :register (assoc acc (:address e)
                                    (assoc e :stars []
                                           :withdrawn []
                                           :next-unlock (java.util.Date. (+ (.getTime (:windup e))
                                                                            (* 1000 (:rate-unit e))))
                                           :current-rate 1))
                   :deposit
                   (let [{:keys [amount
                                 withdrawn
                                 stars
                                 next-unlock
                                 rate
                                 current-rate
                                 rate-unit]} (get acc (:address e))]
                     (if (> amount (inc (+ (count withdrawn) (count stars))))
                       (update-in acc [(:address e) :stars] conj e)
                       (-> (update-in acc [(:address e) :stars] conj e)
                           (update-in
                            [(:address e) :stars]
                            (fn [stars]
                              (vec (rseq (mapv (fn [x u] (assoc x :unlocked-at u))
                                               (rseq stars)
                                               (->> (iterate inc 0)
                                                    (map (fn [y]
                                                           (java.util.Date. (+ (.getTime next-unlock)
                                                                               (* 1000 y rate-unit)))))
                                                    (map (partial repeat rate))
                                                    (mapcat identity)
                                                    (drop (dec current-rate)))))))))))
                   :transfer-batch (-> (assoc acc (:to-address e) (get acc (:from-address e)))
                                       (dissoc (:from-address e)))
                   :withdraw
                   (if (nil? (get acc (:address e)))
                     (do (println (str "strange withdraw: " (:address e)))
                         acc)
                     (let [star (last (:stars (get acc (:address e))))
                           unlocked? (boolean (:unlocked-at star))
                           {:keys [next-unlock rate rate-unit current-rate]} (get acc (:address e))]
                       (-> acc
                           (update-in [(:address e) :withdrawn]
                                      conj
                                      (if unlocked?
                                        (merge e star)
                                        (merge e star {:unlocked-at next-unlock})))
                           (update-in
                            [(:address e) :next-unlock]
                            (if (or unlocked? (not (= current-rate rate)))
                              identity
                              (fn [u] (java.util.Date. (+ (.getTime u) (* 1000 rate-unit))))))
                           (update-in [(:address e) :current-rate]
                                      (if (= current-rate rate) (constantly 1) inc))
                           (update-in [(:address e) :stars] pop))))
                   acc
                   )) {})
       #_remove-joke
        #_vals
       #_(mapcat (fn [e]
                 (map (fn [s] (merge {:lsr/address (:address s)
                                      :lsr/star {:db/id [:node/urbit-id (:star s)]}
                                      :lsr/deposited-at (:deposited-at s)
                                      :lsr/star+deposited-at [{:db/id [:node/urbit-id (:star s)]}
                                                              (:deposited-at s)]}
                                     (when (:unlocked-at s)
                                       {:lsr/unlocked-at (:unlocked-at s)})
                                     (when (:withdrawn-at s)
                                       {:lsr/withdrawn-at (:withdrawn-at s)})))
                      (concat (:stars e) (:withdrawn e)))))))

(defn get-transactions [address]
  (loop [r []
         startblock "0"]
    (let [res (-> (http/get "https://api.etherscan.io/api"
                            {:query-params {:module "account"
                                            :action "txlist"
                                            :address address
                                            :startblock startblock
                                            :endblock "latest"
                                            :page 1
                                            :offset 10000
                                            :sort "asc"
                                            :apikey "GGVBET75PP24QF7G1PSFAIIU1B24MC8BJM"}})
                  :body
                  json/read-str
                  (get "result"))
          block (get (last res) "blockNumber")]
      ;; etherscan api rate limit
      (Thread/sleep 200)
      (if (< (count res) 10000)
        (concat r res)
        (recur (concat r (take-while (fn [e] (not= block (get e "blockNumber"))) res)) block)))))

(defn unpack [{:strs [input hash from timeStamp blockNumber transactionIndex]}]
  (loop [r []
         idx 2]
    (if (>= idx (count input))
      r
      (let [len (* 2 (Long/parseLong (subs input (+ idx 106) (+ idx 170)) 16))
            end-idx (+ idx 170 len)]
        (recur (conj r {"input" (str "0x" (subs input (+ idx 170) end-idx))
                        "from" from
                        "timeStamp" timeStamp
                        "blockNumber" blockNumber
                        "internalIndex" idx
                        "transactionIndex" transactionIndex
                        "hash" hash})
               end-idx)))))

(defn parse-multisend-tx [{:strs [input hash from timeStamp blockNumber transactionIndex]}]
  (if (not= (subs input 0 10) "0x8d80ff0a")
    (throw (ex-info "Invalid method id for parse-multisend-tx" {:method-id (subs input 0 10)}))
    (let [bs-loc (+ 10 (* 2 (Long/parseLong (subs input 10 74) 16)))
          bs-len (* 2 (Long/parseLong (subs input bs-loc (+ 64 bs-loc)) 16))
          bs     (str "0x" (subs input (+ 64 bs-loc) (+ bs-len (+ 64 bs-loc))))]
      (unpack {"input" bs "from" from "timeStamp" timeStamp "blockNumber" blockNumber "transactionIndex" transactionIndex "hash" hash}))))

(defn safe-subs [s start end]
  (try (subs s start end)
       (catch Exception e s)))

(defn parse-gnosis-tx [{:strs [input hash from timeStamp blockNumber transactionIndex]}]
  (if (not= (subs input 0 10) "0x6a761202")
    (throw (ex-info "Invalid method id for parse-gnosis-tx" {:method-id (subs input 0 10)}))
    (let [bs-loc (+ 10 (* 2 (Long/parseLong (subs input 138 202) 16)))
          bs-len (* 2 (Long/parseLong (subs input bs-loc (+ 64 bs-loc)) 16))
          bs     (str "0x" (subs input (+ 64 bs-loc) (+ bs-len (+ 64 bs-loc))))]
      (if (= "0x8d80ff0a" (safe-subs bs 0 10))
        (parse-multisend-tx
         {"input" bs "from" from "timeStamp"
          timeStamp "blockNumber" blockNumber
          "transactionIndex" transactionIndex
          "hash" hash})
        [{"input" bs "from" from "timeStamp"
          timeStamp "blockNumber" blockNumber
          "transactionIndex" transactionIndex
          "hash" hash}]))))

(defn get-all-transactions []
  (->> (mapcat (fn [a]
                 (->> (get-transactions a)
                      (filter (fn [e]
                                (and
                                 (= "0" (get e "isError"))
                                 (= "0x6a761202" (get e "methodId")))))
                      (mapcat parse-gnosis-tx)
                      (remove nil?))) GNOSIS-SAFE-ADDRESSES)
       (concat (filter (fn [e] (= "0" (get e "isError"))) (get-transactions LSR-ADDRESS)))))

#_(defn get-all-transactions []
  (->> (get-transactions TLON-GNOSIS-SAFE-ADDRESS)
       (filter (fn [e] (clojure.string/starts-with? (get e "functionName") "execTransaction")))
       (filter (fn [e] (= "0" (get e "isError"))))
       (mapcat parse-gnosis-tx)
       (remove nil?)
       (concat (filter (fn [e] (= "0" (get e "isError"))) (get-transactions LSR-ADDRESS)))
       (sort-by (fn [e] (parse-long (get e "timeStamp"))))))

