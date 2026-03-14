import { Routes } from '@angular/router';
import { TracksByGroup } from './tracks-by-group/tracks-by-group';
import { TrackEdit } from './track-edit/track-edit';
import { UnclassifiedTracks } from './unclassified-tracks/unclassified-tracks';
import { TrackSearch } from './track-search/track-search';
import { MixingBoard } from './mixing-board/mixing-board';
import { Status } from './status/status';
import { Queue } from './queue/queue';
import { Groups } from './groups/groups';

export const routes: Routes = [
    { path: 'status', component: Status },
    { path: 'groups', component: Groups },
    { path: 'group/:groupId', component: TracksByGroup },
    { path: 'track/:id', component: TrackEdit },
    { path: 'player', redirectTo: '/queue', pathMatch: 'full' },
    { path: 'queue', component: Queue },
    { path: 'unclassified', component: UnclassifiedTracks },
    { path: 'search', component: TrackSearch },
    { path: 'mixer', component: MixingBoard },
    { path: '', redirectTo: '/groups', pathMatch: 'full' }
];
