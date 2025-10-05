package vendaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.filter.VendaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.model.VendaProduto;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.service.CaixaLancamentoService;
import net.originmobi.pdv.service.CaixaService;
import net.originmobi.pdv.service.PagamentoTipoService;
import net.originmobi.pdv.service.ParcelaService;
import net.originmobi.pdv.service.ProdutoService;
import net.originmobi.pdv.service.ReceberService;
import net.originmobi.pdv.service.UsuarioService;
import net.originmobi.pdv.service.VendaProdutoService;
import net.originmobi.pdv.service.VendaService;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendaService — testes unitarios")
class VendaServiceTest {

    @InjectMocks
    private VendaService vendaService;

    @Mock private VendaRepository vendas;
    @Mock private UsuarioService usuarios;
    @Mock private VendaProdutoService vendaProdutos;
    @Mock private PagamentoTipoService formaPagamentos;
    @Mock private CaixaService caixas;
    @Mock private ReceberService receberServ;
    @Mock private ParcelaService parcelas;
    @Mock private CaixaLancamentoService lancamentos;
    @Mock private TituloService tituloService;
    @Mock private CartaoLancamentoService cartaoLancamento;
    @Mock private ProdutoService produtos;

    @Test
    @DisplayName("abreVenda(): se venda não tiver código deve preencher campos padrão, buscar usuário e salvar")
    void abreVenda_quandoSemCodigo_devePreencherCamposBuscarUsuarioESalvar() {
        
        Venda venda = new Venda(); 

        try (MockedStatic<net.originmobi.pdv.singleton.Aplicacao> app =
                 mockStatic(net.originmobi.pdv.singleton.Aplicacao.class)) {

            net.originmobi.pdv.singleton.Aplicacao aplicacaoMock = mock(net.originmobi.pdv.singleton.Aplicacao.class);
            app.when(net.originmobi.pdv.singleton.Aplicacao::getInstancia).thenReturn(aplicacaoMock);
            when(aplicacaoMock.getUsuarioAtual()).thenReturn("natalia");

            
            Usuario usuario = new Usuario();
            usuario.setUser("natalia");
            when(usuarios.buscaUsuario("natalia")).thenReturn(usuario);

            
            when(vendas.save(any(Venda.class))).thenAnswer(inv -> {
                Venda v = inv.getArgument(0);
                v.setCodigo(123L);
                return v;
            });

            
            Long idGerado = vendaService.abreVenda(venda);

            
            assertEquals(123L, idGerado, "deve retornar o código atribuído pelo repository");
            assertEquals(VendaSituacao.ABERTA, venda.getSituacao(), "deve marcar como ABERTA");
            assertEquals(0.00, venda.getValor_produtos(), 0.0001, "deve iniciar valor_produtos em 0.00");
            assertNotNull(venda.getData_cadastro(), "deve preencher data_cadastro");
            assertSame(usuario, venda.getUsuario(), "deve vincular o usuário atual");

            verify(usuarios).buscaUsuario("natalia");
            verify(vendas).save(same(venda));
            verify(vendas, never()).updateDadosVenda(any(), any(), any());
        }
    }
 
    
    @Test
    @DisplayName("abreVenda(): se venda já possuir código deve chamar updateDadosVenda")
    void abreVenda_quandoExisteCodigo_deveAtualizarDados() {
        Venda venda = new Venda();
        venda.setCodigo(10L);
        Pessoa pessoa = new Pessoa();
        venda.setPessoa(pessoa);
        venda.setObservacao("obs");

        Long id = vendaService.abreVenda(venda);

        assertEquals(10L, id);
        verify(vendas).updateDadosVenda(eq(pessoa), eq("obs"), eq(10L));
        verify(vendas, never()).save(any());
    }

    @Test
    @DisplayName("busca(): se filter tiver código deve chamar findByCodigoIn")
    void busca_quandoFilterTemCodigo_deveChamarFindByCodigoIn() {
        VendaFilter filter = new VendaFilter();
        filter.setCodigo(123L);
        Pageable pageable = mock(Pageable.class);
        Page<Venda> page = mock(Page.class);
        when(vendas.findByCodigoIn(123L, pageable)).thenReturn(page);

        Page<Venda> result = vendaService.busca(filter, "ABERTA", pageable);

        assertSame(page, result);
        verify(vendas, never()).findBySituacaoEquals(any(), any());
    }

    @Test
    @DisplayName("busca(): se filter não tiver código deve chamar findBySituacaoEquals(ABERTA)")
    void busca_quandoFilterSemCodigo_deveChamarFindBySituacao() {
        VendaFilter filter = new VendaFilter();
        Pageable pageable = mock(Pageable.class);
        Page<Venda> page = mock(Page.class);
        when(vendas.findBySituacaoEquals(VendaSituacao.ABERTA, pageable)).thenReturn(page);

        Page<Venda> result = vendaService.busca(filter, "ABERTA", pageable);

        assertSame(page, result);
        verify(vendas).findBySituacaoEquals(eq(VendaSituacao.ABERTA), eq(pageable));
    }
    
    @Test
    @DisplayName("busca(): se situacao != 'ABERTA' deve chamar findBySituacaoEquals(FECHADA)")
    void busca_quandoSituacaoFechada_deveChamarFindBySituacaoFechada() {
        VendaFilter filter = new VendaFilter(); // sem código
        Pageable pageable = mock(Pageable.class);
        Page<Venda> page = mock(Page.class);
        when(vendas.findBySituacaoEquals(VendaSituacao.FECHADA, pageable)).thenReturn(page);

        Page<Venda> result = vendaService.busca(filter, "FECHADA", pageable);

        assertSame(page, result);
        verify(vendas).findBySituacaoEquals(eq(VendaSituacao.FECHADA), eq(pageable));
    }

    @Test
    @DisplayName("addProduto(): se venda estiver ABERTA deve salvar produto e retornar 'ok'")
    void addProduto_quandoVendaAberta_deveSalvarERetornarOk() {
        when(vendas.verificaSituacao(1L)).thenReturn(VendaSituacao.ABERTA.toString());

        String r = vendaService.addProduto(1L, 2L, 10.5);

        assertEquals("ok", r);
        verify(vendaProdutos).salvar(any(VendaProduto.class));
    }

    @Test
    @DisplayName("addProduto(): se venda estiver FECHADA deve retornar 'Venda fechada'")
    void addProduto_quandoVendaFechada_deveRetornarMensagemVendaFechada() {
        when(vendas.verificaSituacao(1L)).thenReturn(VendaSituacao.FECHADA.toString());

        String r = vendaService.addProduto(1L, 2L, 10.5);

        assertEquals("Venda fechada", r);
        verify(vendaProdutos, never()).salvar(any());
    }

    @Test
    @DisplayName("removeProduto(): se venda estiver ABERTA deve remover produto e retornar 'ok'")
    void removeProduto_quandoVendaAberta_deveRemoverERetornarOk() {
        Venda venda = mock(Venda.class);
        when(vendas.findByCodigoEquals(99L)).thenReturn(venda);
        when(venda.getSituacao()).thenReturn(VendaSituacao.ABERTA);

        String r = vendaService.removeProduto(3L, 99L);

        assertEquals("ok", r);
        verify(vendaProdutos).removeProduto(3L);
    }

    @Test
    @DisplayName("removeProduto(): se venda estiver FECHADA deve retornar 'Venda fechada'")
    void removeProduto_quandoVendaFechada_deveRetornarMensagemVendaFechada() {
        Venda venda = mock(Venda.class);
        when(vendas.findByCodigoEquals(99L)).thenReturn(venda);
        when(venda.getSituacao()).thenReturn(VendaSituacao.FECHADA);

        String r = vendaService.removeProduto(3L, 99L);

        assertEquals("Venda fechada", r);
        verify(vendaProdutos, never()).removeProduto(anyLong());
    }

    @Test
    @DisplayName("lista(): deve retornar vendas do repository (findAll)")
    void lista_deveDelegarAoRepository() {
        List<Venda> todas = Arrays.asList(new Venda(), new Venda());
        when(vendas.findAll()).thenReturn(todas);

        assertEquals(todas, vendaService.lista());
    }


    @Test
    @DisplayName("qtdAbertos(): deve retornar total de vendas em aberto")
    void qtdAbertos_deveRetornarTotalDeVendasEmAberto() {
        when(vendas.qtdVendasEmAberto()).thenReturn(42);
        assertEquals(42, vendaService.qtdAbertos());
    }

}
