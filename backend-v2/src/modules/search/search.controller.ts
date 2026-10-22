import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { SearchService } from './search.service';
import { SearchQueryDto } from './dto/search-query.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';

@ApiTags('search')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('search')
export class SearchController {
  constructor(private readonly searchService: SearchService) {}

  @Get()
  @ApiOperation({
    summary: 'Global search across users, posts, events, clubs, accommodation & marketplace',
    description: 'Uses PostgreSQL trigram (pg_trgm) LIKE matching. Filter to a specific entity type with `?entity=users` etc.',
  })
  async search(@Query() dto: SearchQueryDto) {
    return this.searchService.search(dto);
  }
}
