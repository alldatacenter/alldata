import {
  PermissionLevels,
  ResourceTypes,
  VizResourceSubTypes,
} from '../PermissionPage/constants';

export function allowCreateStoryboard() {
  return {
    module: ResourceTypes.Viz,
    id: VizResourceSubTypes.Storyboard,
    level: PermissionLevels.Create,
  };
}

export function allowManageStoryboard(id?: string) {
  return {
    module: ResourceTypes.Viz,
    id,
    level: PermissionLevels.Manage,
  };
}
