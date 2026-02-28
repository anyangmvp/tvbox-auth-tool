package anyang.tvbox.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.util.Map;

public class ServiceListAdapter extends BaseAdapter {
    private Context context;
    private List<String> services;
    private LayoutInflater inflater;

    public ServiceListAdapter(Context context, List<String> services) {
        this.context = context;
        this.services = services;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return services.size();
    }

    @Override
    public Object getItem(int position) {
        return services.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_service, parent, false);
            holder = new ViewHolder();
            holder.itemIcon = convertView.findViewById(R.id.item_icon);
            holder.itemName = convertView.findViewById(R.id.item_name);
            holder.itemDescription = convertView.findViewById(R.id.item_description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String serviceName = services.get(position);
        holder.itemName.setText(serviceName);
        holder.itemDescription.setText("点击访问 " + serviceName);

        // 根据服务名称设置不同的图标颜色
        int iconResource = getIconForService(serviceName);
        holder.itemIcon.setImageResource(iconResource);

        return convertView;
    }

    private int getIconForService(String serviceName) {
        // 可以根据不同的服务返回不同的图标
        if (serviceName.toLowerCase().contains("tv")) {
            return R.drawable.ic_home;
        } else if (serviceName.toLowerCase().contains("video")) {
            return R.drawable.ic_list;
        } else if (serviceName.toLowerCase().contains("movie")) {
            return R.drawable.ic_key;
        }
        return R.drawable.ic_home;
    }

    static class ViewHolder {
        ImageView itemIcon;
        TextView itemName;
        TextView itemDescription;
    }
}
