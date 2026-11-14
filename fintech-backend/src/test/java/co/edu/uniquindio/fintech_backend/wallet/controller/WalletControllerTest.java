package co.edu.uniquindio.fintech_backend.wallet.controller;

import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.service.WalletCommands;
import co.edu.uniquindio.fintech_backend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WalletExceptionHandler.class)
class WalletControllerTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final Authentication authenticatedOwner = UsernamePasswordAuthenticationToken.authenticated(OWNER.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private WalletCommands commands;
    @MockitoBean
    private WalletService service;

    @Test
    void shouldCreateWalletFromAuthenticatedOwner() throws Exception {
        Wallet wallet = new Wallet(OWNER, "Savings", WalletType.SAVINGS);
        when(commands.create(OWNER, "Savings", WalletType.SAVINGS)).thenReturn(wallet);
        mvc.perform(post("/api/wallets").principal(authenticatedOwner).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Savings\",\"type\":\"SAVINGS\"}")).andExpect(status().isCreated()).andExpect(header().string("Location", "/api/wallets/" + wallet.getId())).andExpect(jsonPath("$.ownerId").value(OWNER.toString())).andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void shouldListAndReadOnlyOwnWallets() throws Exception {
        Wallet wallet = new Wallet(OWNER, "Savings", WalletType.SAVINGS);
        when(service.getMine(OWNER)).thenReturn(List.of(wallet));
        when(service.getMineById(OWNER, wallet.getId())).thenReturn(wallet);
        mvc.perform(get("/api/wallets").principal(authenticatedOwner)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/wallets/{id}", wallet.getId()).principal(authenticatedOwner)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Savings"));
    }

    @Test
    void shouldRejectInvalidWalletRequest() throws Exception {
        mvc.perform(post("/api/wallets").principal(authenticatedOwner).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"type\":\"SAVINGS\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void shouldReturnNotFoundForUnownedWallet() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getMineById(OWNER, id)).thenThrow(new NoSuchElementException("Wallet not found"));
        mvc.perform(get("/api/wallets/{id}", id).principal(authenticatedOwner)).andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateAndDeactivateWallet() throws Exception {
        Wallet wallet = new Wallet(OWNER, "Savings", WalletType.SAVINGS);
        wallet.updateDetails("Travel", WalletType.TRANSPORT);
        when(commands.update(OWNER, wallet.getId(), "Travel", WalletType.TRANSPORT)).thenReturn(wallet);
        mvc.perform(put("/api/wallets/{id}", wallet.getId()).principal(authenticatedOwner).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Travel\",\"type\":\"TRANSPORT\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Travel"));
        wallet.deactivate();
        when(commands.deactivate(OWNER, wallet.getId())).thenReturn(wallet);
        mvc.perform(patch("/api/wallets/{id}/deactivate", wallet.getId()).principal(authenticatedOwner)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
