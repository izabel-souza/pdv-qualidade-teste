package notafiscalService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalRepository;
import net.originmobi.pdv.service.notafiscal.NotaFiscalService;

class NotaFiscalServiceTest {

    @Test
    void deveRetornarListaDeNotasFiscais() {
        // cria mock do repository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);

        // cria objeto da classe service
        NotaFiscalService service = new NotaFiscalService();

        // injeta o mock no campo private notasFiscais
        ReflectionTestUtils.setField(service, "notasFiscais", mockNFeRepo);

        // instancia notas fiscais para simulação
        NotaFiscal nf1 = new NotaFiscal();
        NotaFiscal nf2 = new NotaFiscal();
        List<NotaFiscal> listaEsperada = Arrays.asList(nf1, nf2);

        // define comportamento do mock
        when(mockNFeRepo.findAll()).thenReturn(listaEsperada);

        // executa
        List<NotaFiscal> resultado = service.lista();

        // validações
        assertEquals(2, resultado.size(), "Deveria retornar 2 notas fiscais");
        assertEquals(listaEsperada, resultado, "A lista retornada deve ser igual à esperada");

        // verifica que o mock foi chamado
        verify(mockNFeRepo, times(1)).findAll();

        System.out.println("✅ Teste passou: lista() retornou os valores esperados");
    }

    @Test
    void deveRetornarTotalDeNotasEmitidas() {
        // cria mock do repository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);
        NotaFiscalService service = new NotaFiscalService();

        // injeta mock
        ReflectionTestUtils.setField(service, "notasFiscais", mockNFeRepo);

        // define comportamento
        when(mockNFeRepo.totalNotaFiscalEmitidas()).thenReturn(5);

        // executa
        int total = service.totalNotaFiscalEmitidas();

        // valida
        assertEquals(5, total, "O total de notas emitidas deve ser 5");
        verify(mockNFeRepo, times(1)).totalNotaFiscalEmitidas();
    }

    @Test
    void deveBuscarNotaFiscalPorCodigo() {
        // cria mock do repository
        NotaFiscalRepository mockNFeRepo = mock(NotaFiscalRepository.class);
        NotaFiscalService service = new NotaFiscalService();

        // injeta mock
        ReflectionTestUtils.setField(service, "notasFiscais", mockNFeRepo);

        // prepara simulação
        NotaFiscal nf = new NotaFiscal();
        nf.setCodigo(123L);
        when(mockNFeRepo.findById(123L)).thenReturn(Optional.of(nf));

        // executa
        Optional<NotaFiscal> resultado = service.busca(123L);

        // valida
        assertTrue(resultado.isPresent(), "A nota deveria estar presente");
        assertEquals(123L, resultado.get().getCodigo());
        verify(mockNFeRepo, times(1)).findById(123L);
    }
    
    @Test
    void deveGerarDVCorretamente() {
        NotaFiscalService service = new NotaFiscalService();

        // Exemplo simples: código "1234567890"
        int dv = service.geraDV("1234567890");

        assertTrue(dv >= 0 && dv <= 9, "DV deve estar entre 0 e 9");
    }

    @Test
    void deveRetornarZeroParaEntradaInvalida() {
        NotaFiscalService service = new NotaFiscalService();

        int dv = service.geraDV(null); // deve cair no catch
        assertEquals(0, dv);
    }

    
    @Test
    void deveSalvarArquivoXML() throws Exception {
        NotaFiscalService service = new NotaFiscalService();

        String xml = "<nfe>conteudo</nfe>";
        String chave = "teste123";

        // chama método
        service.salvaXML(xml, chave);

        // verifica se o arquivo foi criado
        File file = new File(new File(".").getCanonicalPath() + "/src/main/resources/xmlNfe/" + chave + ".xml");
        assertTrue(file.exists(), "O arquivo XML deveria ter sido criado");

        // cleanup
        file.delete();
    }

    @Test
    void deveRemoverArquivoXMLExistente() throws Exception {
        NotaFiscalService service = new NotaFiscalService();

        String chave = "remover123";
        File file = new File(new File(".").getCanonicalPath() + "/src/main/resources/xmlNfe/" + chave + ".xml");

        // cria arquivo manualmente
        file.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("<nfe>teste</nfe>");
        }

        assertTrue(file.exists(), "Arquivo de teste deveria existir");

        // chama método
        service.removeXml(chave);

        // verifica se foi removido
        assertFalse(file.exists(), "Arquivo deveria ter sido removido");
    }
    
    
    //ERRO
    @Test
    void deveEmitirNotaFiscalComChaveDeAcesso() {
        NotaFiscalRepository mockRepo = mock(NotaFiscalRepository.class);
        NotaFiscalService service = new NotaFiscalService();
        ReflectionTestUtils.setField(service, "notasFiscais", mockRepo);

        NotaFiscal nf = new NotaFiscal();

        // simula salvar retornando a própria nota
        when(mockRepo.save(any(NotaFiscal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // executa
        service.emitir(nf);

        // verifica que a nota foi salva com chave não nula
        assertNotNull(nf.getChave_acesso(), "A chave de acesso deveria ter sido preenchida");
        verify(mockRepo, times(1)).save(nf);
    }




}
