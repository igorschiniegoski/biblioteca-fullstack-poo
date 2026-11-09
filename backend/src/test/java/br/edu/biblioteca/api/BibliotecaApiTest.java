package br.edu.biblioteca.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a integração completa Controller → Service → Domain → Repository → JPA → banco (H2),
 * fazendo requisições HTTP reais contra a aplicação.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BibliotecaApiTest {

    private static final HttpClient CLIENTE = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${local.server.port}")
    private int porta;

    @Test
    void fluxoCompletoDeEmprestimoEDevolucao() throws Exception {
        long leitorId = json(post("/api/leitores", """
                {"nome":"Ana Souza","email":"ana@email.com","telefone":"(42) 99999-0000"}
                """)).get("id").asLong();
        long livroId = json(post("/api/livros", """
                {"isbn":"978-85-359-0277-8","titulo":"Dom Casmurro","autor":"Machado de Assis","anoPublicacao":1899}
                """)).get("id").asLong();
        var exemplarResp = post("/api/livros/" + livroId + "/exemplares", """
                {"codigo":"DC-001"}
                """);
        assertEquals(201, exemplarResp.statusCode());
        long exemplarId = json(exemplarResp).get("id").asLong();

        var emprestimo = post("/api/emprestimos", "{\"leitorId\":" + leitorId + ",\"exemplarId\":" + exemplarId + "}");
        assertEquals(201, emprestimo.statusCode());
        var emprestimoJson = json(emprestimo);
        assertEquals("ATIVO", emprestimoJson.get("situacao").asText());
        assertEquals("Dom Casmurro", emprestimoJson.get("livroTitulo").asText());
        assertEquals("/api/emprestimos/" + emprestimoJson.get("id").asLong(),
                emprestimo.headers().firstValue("Location").orElseThrow());

        var livro = json(get("/api/livros/" + livroId));
        assertEquals(0, livro.get("exemplaresDisponiveis").asInt());

        var devolucao = patch("/api/emprestimos/" + emprestimoJson.get("id").asLong() + "/devolucao");
        assertEquals(200, devolucao.statusCode());
        assertEquals("DEVOLVIDO", json(devolucao).get("situacao").asText());
        assertEquals(1, json(get("/api/livros/" + livroId)).get("exemplaresDisponiveis").asInt());
        assertEquals(1, json(get("/api/leitores/" + leitorId + "/emprestimos")).size());
    }

    @Test
    void naoEmprestaExemplarJaEmprestadoERetorna409() throws Exception {
        long livroId = json(post("/api/livros", """
                {"isbn":"9788576082675","titulo":"Código Limpo","autor":"Robert C. Martin"}
                """)).get("id").asLong();
        long exemplarId = json(post("/api/livros/" + livroId + "/exemplares", "{\"codigo\":\"CL-001\"}")).get("id").asLong();
        long ana = json(post("/api/leitores", "{\"nome\":\"Ana\",\"email\":\"ana@email.com\"}")).get("id").asLong();
        long bruno = json(post("/api/leitores", "{\"nome\":\"Bruno\",\"email\":\"bruno@email.com\"}")).get("id").asLong();

        assertEquals(201, post("/api/emprestimos", "{\"leitorId\":" + ana + ",\"exemplarId\":" + exemplarId + "}").statusCode());
        var recusado = post("/api/emprestimos", "{\"leitorId\":" + bruno + ",\"exemplarId\":" + exemplarId + "}");

        assertEquals(409, recusado.statusCode());
        assertEquals("O exemplar CL-001 já está emprestado.", json(recusado).get("mensagem").asText());
    }

    @Test
    void dadosInvalidosRetornam400ComOsCampos() throws Exception {
        var resposta = post("/api/leitores", "{\"nome\":\"\",\"email\":\"nao-e-email\"}");

        assertEquals(400, resposta.statusCode());
        var campos = json(resposta).get("campos");
        assertTrue(campos.has("nome"));
        assertTrue(campos.has("email"));
        assertEquals(400, post("/api/leitores", "{nome:").statusCode());
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        assertEquals(404, get("/api/leitores/999").statusCode());
        var resposta = post("/api/emprestimos", "{\"leitorId\":999,\"exemplarId\":1}");
        assertEquals(404, resposta.statusCode());
        assertEquals("Leitor 999 não encontrado(a).", json(resposta).get("mensagem").asText());
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        post("/api/leitores", "{\"nome\":\"Ana\",\"email\":\"ana@email.com\"}");

        var duplicado = post("/api/leitores", "{\"nome\":\"Outra Ana\",\"email\":\"ANA@email.com\"}");

        assertEquals(409, duplicado.statusCode());
    }

    @Test
    void naoExcluiLivroComHistoricoMasExcluiLeitorSemEmprestimos() throws Exception {
        long livroId = json(post("/api/livros", """
                {"isbn":"9788550804606","titulo":"Use a Cabeça! Java","autor":"Kathy Sierra"}
                """)).get("id").asLong();
        long exemplarId = json(post("/api/livros/" + livroId + "/exemplares", "{\"codigo\":\"JAVA-001\"}")).get("id").asLong();
        long leitorComHistorico = json(post("/api/leitores", "{\"nome\":\"Ana\",\"email\":\"ana@email.com\"}")).get("id").asLong();
        long leitorNovo = json(post("/api/leitores", "{\"nome\":\"Bia\",\"email\":\"bia@email.com\"}")).get("id").asLong();
        long emprestimoId = json(post("/api/emprestimos",
                "{\"leitorId\":" + leitorComHistorico + ",\"exemplarId\":" + exemplarId + "}")).get("id").asLong();
        patch("/api/emprestimos/" + emprestimoId + "/devolucao");

        assertEquals(409, delete("/api/livros/" + livroId).statusCode());
        assertEquals(409, delete("/api/leitores/" + leitorComHistorico).statusCode());
        assertEquals(204, delete("/api/leitores/" + leitorNovo).statusCode());
        assertEquals(404, get("/api/leitores/" + leitorNovo).statusCode());
    }

    @Test
    void atualizaLeitorComPut() throws Exception {
        long id = json(post("/api/leitores", "{\"nome\":\"Ana\",\"email\":\"ana@email.com\"}")).get("id").asLong();

        var atualizado = send(HttpRequest.newBuilder(uri("/api/leitores/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"nome\":\"Ana Souza\",\"email\":\"ana.souza@email.com\"}")));

        assertEquals(200, atualizado.statusCode());
        assertEquals("Ana Souza", json(atualizado).get("nome").asText());
    }

    private URI uri(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        return send(HttpRequest.newBuilder(uri(caminho)).GET());
    }

    private HttpResponse<String> post(String caminho, String corpo) throws Exception {
        return send(HttpRequest.newBuilder(uri(caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)));
    }

    private HttpResponse<String> patch(String caminho) throws Exception {
        return send(HttpRequest.newBuilder(uri(caminho)).method("PATCH", HttpRequest.BodyPublishers.noBody()));
    }

    private HttpResponse<String> delete(String caminho) throws Exception {
        return send(HttpRequest.newBuilder(uri(caminho)).DELETE());
    }

    private HttpResponse<String> send(HttpRequest.Builder requisicao) throws Exception {
        return CLIENTE.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> resposta) {
        return JSON.readTree(resposta.body());
    }
}
