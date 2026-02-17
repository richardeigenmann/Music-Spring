import { Routes } from '@angular/router';
import { Playlists } from './playlists/playlists';
import { TracksByGroup } from './tracks-by-group/tracks-by-group';
import { TrackEdit } from './track-edit/track-edit';

export const routes: Routes = [
    { path: 'playlists', component: Playlists },
    { path: 'group/:groupId', component: TracksByGroup },
    { path: 'track/:id', component: TrackEdit },
    { path: '', redirectTo: '/playlists', pathMatch: 'full' }
];
