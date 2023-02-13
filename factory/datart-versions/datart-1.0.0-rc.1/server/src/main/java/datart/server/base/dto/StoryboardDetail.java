package datart.server.base.dto;

import datart.core.entity.Storyboard;
import datart.core.entity.Storypage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoryboardDetail extends Storyboard {

    List<Storypage> storypages;

    private boolean download;

}
