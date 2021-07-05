package outerparkdz;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="dramas", path="dramas")
public interface DramaRepository extends PagingAndSortingRepository<Drama, Long>{

    Drama findByDramaId(Long dramaId);

}
