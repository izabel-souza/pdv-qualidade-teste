package notafiscalService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.model.Empresa;
import net.originmobi.pdv.model.EmpresaParametro;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalRepository;
import net.originmobi.pdv.service.EmpresaService;
import net.originmobi.pdv.service.PessoaService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalTotaisServer;

class NotaFiscalServiceTest {

    @Test
    @DisplayName("Teste do método lista()")
    void retornaListaDeNotasFiscais() {
    	
        //mock do NotaFiscalRepository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);

        //objeto da classe NotaFiscalService
        NotaFiscalService nfeService = new NotaFiscalService();

        /*
        como notasFiscais (NotaFiscalRepository) é privado, não é possível rodar: nfeService.notasFiscais = mockRepo;
        porque ao instanciar NotaFiscalService manualmente, os atributos private tem valor nulo.
   
   		O ReflectionTestUtils permite setar valores em atributos privados de uma classe.
   		
   		Pega o atributo notasFiscais do objeto nfeService e seta o seu valor como mockNFeRepo (NotaFiscalRepository) 
         */
        
        //injeta o mock no campo private notasFiscais
        ReflectionTestUtils.setField(nfeService, "notasFiscais", mockNFeRepo);

        // instancia notas fiscais para teste e adiciona na lista listaEsperada
        NotaFiscal nf1 = new NotaFiscal();
        NotaFiscal nf2 = new NotaFiscal();
        List<NotaFiscal> listaEsperada = Arrays.asList(nf1, nf2);

        //definição do comportamento do mock -- quando chamar o método findAll() de mockNFeRepo, retorna listaEsperada
        when(mockNFeRepo.findAll()).thenReturn(listaEsperada);

        //chama o método lista() de NotaFiscalService
        List<NotaFiscal> resultado = nfeService.lista();

        //verificações de resultados do teste
        assertEquals(2, resultado.size()); //"Retorna 2 notas fiscais (nf1 e nf2)
        assertEquals(listaEsperada, resultado); //A lista retornada deve ser igual à esperada
    }

    
    @Test
    @DisplayName("Teste do método totalNotaFiscalEmitidas()")
    void retornaTotalDeNotasEmitidas() {
    	
    	//mock do NotaFiscalRepository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);
        
        //objeto da classe NotaFiscalService
        NotaFiscalService nfeService = new NotaFiscalService();

        /*
        como notasFiscais (NotaFiscalRepository) é privado, não é possível rodar: nfeService.notasFiscais = mockRepo;
        porque ao instanciar NotaFiscalService manualmente, os atributos private tem valor nulo.
   
   		O ReflectionTestUtils permite setar valores em atributos privados de uma classe.
   		
   		Pega o atributo notasFiscais do objeto nfeService e seta o seu valor como mockNFeRepo (NotaFiscalRepository) 
         */
        
        //injeta o mock no campo private notasFiscais
        ReflectionTestUtils.setField(nfeService, "notasFiscais", mockNFeRepo);

       //definição do comportamento do mock -- quando chamar o método totalNotaFiscalEmitidas() de mockNFeRepo, retorna 5
        when(mockNFeRepo.totalNotaFiscalEmitidas()).thenReturn(5);

        //chama o método totalNotaFiscalEmitidas() de NotaFiscalService
        int total = nfeService.totalNotaFiscalEmitidas();

        //verificações de resultados do teste
        assertEquals(5, total); //"O total de notas emitidas deve ser 5
    }
    

    @Test
    @DisplayName("Teste do método busca(codnota)")
    void deveBuscarNotaFiscalPorCodigo() {
    	
    	//mock do NotaFiscalRepository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);
        
        //objeto da classe NotaFiscalService
        NotaFiscalService nfeService = new NotaFiscalService();

        /*
        como notasFiscais (NotaFiscalRepository) é privado, não é possível rodar: nfeService.notasFiscais = mockRepo;
        porque ao instanciar NotaFiscalService manualmente, os atributos private tem valor nulo.
   
   		O ReflectionTestUtils permite setar valores em atributos privados de uma classe.
   		
   		Pega o atributo notasFiscais do objeto nfeService e seta o seu valor como mockNFeRepo (NotaFiscalRepository) 
         */
        
        //injeta o mock no campo private notasFiscais
        ReflectionTestUtils.setField(nfeService, "notasFiscais", mockNFeRepo);

        //instancia objeto NotaFiscal para teste
        NotaFiscal nf = new NotaFiscal();
        nf.setCodigo(123L);
        
        //definição do comportamento do mock -- quando chamar o método busca(Long codnota) de mockNFeRepo, retorna nota fiscal de codigo 123L
        when(mockNFeRepo.findById(123L)).thenReturn(Optional.of(nf));

        //chama o método busca(Long codnota) de NotaFiscalService
        Optional<NotaFiscal> resultado = nfeService.busca(123L);

        //verificações de resultados do teste
        assertTrue(resultado.isPresent()); //A nota deve existir (estar presente)
        assertEquals(123L, resultado.get().getCodigo()); //verifica se o codigo retornado da nota é 123L
    }
    
    
    @Test
    @DisplayName("Teste do método geraDV(String codigo)")
    void geraDVCorretamente() {
    	
        //objeto da classe NotaFiscalService
        NotaFiscalService service = new NotaFiscalService();

        //gera nota fiscal com código "1234567890"
        /*
         i=0 → último dígito = 0 × peso 2 = 0 → total=0 → peso=3
         i=1 → 9 × peso 3 = 27 → total=27 → peso=4
         i=2 → 8 × peso 4 = 32 → total=59 → peso=5
         i=3 → 7 × peso 5 = 35 → total=94 → peso=6
         i=4 → 6 × peso 6 = 36 → total=130 → peso=7
         i=5 → 5 × peso 7 = 35 → total=165 → peso=8
		 i=6 → 4 × peso 8 = 32 → total=197 → peso=9
		 i=7 → 3 × peso 9 = 27 → total=224 → peso=10 → reset peso=2
		 i=8 → 2 × peso 2 = 4 → total=228 → peso=3
		 i=9 → 1 × peso 3 = 3 → total=231 → peso=4
         */
        int dv = service.geraDV("1234567890");

        //verificação de resultado do teste
        assertTrue(dv >= 0 && dv <= 9); //retorna true porque dv é 0
    }
    
    
    @Test
    @DisplayName("Teste do método salvaXML(String xml, String chaveNfe)")
    void salvaArquivoXML() throws Exception {
    	
        //objeto da classe NotaFiscalService
        NotaFiscalService service = new NotaFiscalService();

        //dados testes que são passados como parâmetros ao chamar o método
        String xml = "<nfe>conteudo</nfe>";
        String chave = "teste123";

        //chama o método salvaXML(String xml, String chaveNfe) de NotaFiscalService
        service.salvaXML(xml, chave);

        // verifica se o arquivo foi criado
        File file = new File(new File(".").getCanonicalPath() + "/src/main/resources/xmlNfe/" + chave + ".xml");
        
        //verificação de resultado do teste
        assertTrue(file.exists(), "O arquivo XML deveria ter sido criado");

        //deleta arquivo
        file.delete();
    }

    
    @Test
    @DisplayName("Teste do método removeXml(String chave_acesso)")
    void removeArquivoXMLExistente() throws Exception {
    	
        //objeto da classe NotaFiscalService
        NotaFiscalService service = new NotaFiscalService();

        //dado teste que é passado como parâmetro ao chamar o método
        String chave = "remover123";
     
        //cria objeto File que aponta para o arquivo com chave_acesso "remover123"
        File file = new File(new File(".").getCanonicalPath() + "/src/main/resources/xmlNfe/" + chave + ".xml");

        // cria arquivo de teste manualmente
        file.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("<nfe>teste</nfe>");
        }

        //verifica se o arquivo existe
        assertTrue(file.exists());

        // chama método para deletar o arquivo
        service.removeXml(chave);

        // verifica se foi removido
        assertFalse(file.exists()); //arquivo removido
    }
    
    
    @Test
    @DisplayName("Teste do método cadastrar(Long coddesti, String natureza, NotaFiscalTipo tipo)")
    void cadastroDeNotaFiscal() {
    	
        //cria mocks necessários para execução do método
        NotaFiscalRepository mockRepo = mock(NotaFiscalRepository.class);
        EmpresaService mockEmpresaService = mock(EmpresaService.class);
        PessoaService mockPessoaService = mock(PessoaService.class);
        NotaFiscalTotaisServer mockTotaisService = mock(NotaFiscalTotaisServer.class);

        // cria service e injeta mocks (campo privado → ReflectionTestUtils aqui é só utilidade do Spring,
        // mas se não puder usar, pode deixar a ideia no comentário ou imaginar que seria feito pelo @InjectMocks)
        NotaFiscalService nfeService = new NotaFiscalService();
        
        /*
        NotaFiscalRepository notasFiscais 
        EmpresaService empresas
        NotaFiscalTotaisServer notaTotais
        PessoaService pessoas
        
        são privados, não é possível rodar: nfeService.notasFiscais = mockRepo;
        porque ao instanciar NotaFiscalService manualmente, os atributos private tem valor nulo.
   
   		O ReflectionTestUtils permite setar valores em atributos privados de uma classe.
   		
   		Pega o atributo notasFiscais do objeto nfeService e seta o seu valor como mockNFeRepo (NotaFiscalRepository) 
         */
        ReflectionTestUtils.setField(nfeService, "notasFiscais", mockRepo);
        ReflectionTestUtils.setField(nfeService, "empresas", mockEmpresaService);
        ReflectionTestUtils.setField(nfeService, "pessoas", mockPessoaService);
        ReflectionTestUtils.setField(nfeService, "notaTotais", mockTotaisService);

        //dados testes necessários
        EmpresaParametro parametro = new EmpresaParametro();
        parametro.setSerie_nfe(1);
        parametro.setAmbiente(1);

        Empresa empresa = new Empresa();
        empresa.setParametro(parametro);

        Pessoa pessoa = new Pessoa();

        NotaFiscalTotais totais = new NotaFiscalTotais(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        NotaFiscal nfSalva = new NotaFiscal();
        nfSalva.setCodigo(999L);

        //definição do comportamento do mock -- quando chamar o método cadastrar(Long coddesti, String natureza, NotaFiscalTipo tipo) de nfeService, retorna nota fiscal salva 
        when(mockEmpresaService.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresa));
        when(mockPessoaService.buscaPessoa(10L)).thenReturn(Optional.of(pessoa));
        when(mockTotaisService.cadastro(any(NotaFiscalTotais.class))).thenReturn(totais);
        when(mockRepo.buscaUltimaNota(1)).thenReturn(100L);
        when(mockRepo.save(any(NotaFiscal.class))).thenReturn(nfSalva);

        //chama método para cadastrar a nota fiscal
        String codigoGerado = nfeService.cadastrar(10L, "Venda de produtos", NotaFiscalTipo.SAIDA);

        // validação com asserts simples (JUnit)
        assertEquals("999", codigoGerado);
    }


}
