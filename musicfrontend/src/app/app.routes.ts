import { Routes } from '@angular/router';
import { TracksByTag } from './tracks-by-tag/tracks-by-tag';
import { TrackEdit } from './track-edit/track-edit';
import { UnclassifiedTracks } from './unclassified-tracks/unclassified-tracks';
import { TrackSearch } from './track-search/track-search';
import { MixingBoard } from './mixing-board/mixing-board';
import { Status } from './status/status';
import { Queue } from './queue/queue';
import { Tags } from './tags/tags';
import { Stats } from './stats/stats';

export const routes: Routes = [
    { path: 'status', component: Status },
    { path: 'stats', component: Stats },
    { path: 'tags', component: Tags },
    { path: 'tag/:tagId', component: TracksByTag },
    { path: 'track/:id', component: TrackEdit },
    { path: 'player', redirectTo: '/queue', pathMatch: 'full' },
    { path: 'queue', component: Queue },
    { path: 'unclassified', component: UnclassifiedTracks },
    { path: 'search', component: TrackSearch },
    { path: 'mixer', component: MixingBoard },
    { path: '', redirectTo: '/tags', pathMatch: 'full' }
];
