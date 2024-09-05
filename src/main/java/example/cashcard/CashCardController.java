package example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.swing.text.html.Option;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {
    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        Optional<CashCard> cashCardOptional = findCashCard(requestedId, principal);
        return cashCardOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb,
                                                Principal principal) { // UriComponentsBuilder is provided via DI
        // return null; // note: Spring Web automatically returns a 200 OK response if method returns nothing!
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);
        URI locationOfNewCashCard = ucb.path("cashcards/{id}").buildAndExpand(savedCashCard.id()).toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping
    private ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable, Principal principal) { // Pageable is provided via DI
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(), PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
        ));
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> updateCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate,
                                                Principal principal) {
        Optional<CashCard> cashCardOptional = findCashCard(requestedId, principal);
        /* Note: when trying to write Java in Scala style, e.g. to handle effects (e.g. optionals), it is way more clunky:
         - there is no built-in for comprehension
         - even when using map, since statements aren't expressions, you have to wrap in {}
         - returned type inference can get messier, as per comment below!

        The alternative to this functional style is to define the findCashCard helper method to return CashCard,
        and check the result of this method with if (result != null) { return ... } else { return ... }
        */
        return cashCardOptional.<ResponseEntity<Void>>map(cashCard -> { // casting of map to <ResponseEntity<Void>>
            // is necessary to compile in this case! Otherwise, the expected type is <ResponseEntity<Object>>
                CashCard cashCardUpdateWithOwner = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
                cashCardRepository.save(cashCardUpdateWithOwner);
                return ResponseEntity.noContent().build();
            }
        ).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{requestedId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId, Principal principal) {
        if (!cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())) {
            return ResponseEntity.notFound().build();
        }
        cashCardRepository.deleteById(requestedId);
        return ResponseEntity.noContent().build();
    }

    private Optional<CashCard> findCashCard(Long requestedId, Principal principal) {
        return Optional.ofNullable(cashCardRepository.findByIdAndOwner(requestedId, principal.getName()));
    }
}
